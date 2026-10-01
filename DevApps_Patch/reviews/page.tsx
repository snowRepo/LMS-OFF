"use client";

import { useEffect, useState } from 'react';
import { supabase } from '@/lib/supabaseClient';
import styles from './Reviews.module.css';

export default function ReviewsPage() {
  const [user, setUser] = useState<any>(null);
  const [profile, setProfile] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [needsOnboarding, setNeedsOnboarding] = useState(false);
  
  const [displayName, setDisplayName] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  
  const [selectedProduct, setSelectedProduct] = useState('ShopDesk');

  // Phase 3 State
  const [reviewContent, setReviewContent] = useState('');
  const [reviewsList, setReviewsList] = useState<any[]>([]);
  const [bannedWordsList, setBannedWordsList] = useState<string[]>([]);
  const [isPosting, setIsPosting] = useState(false);
  const [postError, setPostError] = useState('');
  
  // Rate limiting state
  const [lastPostTime, setLastPostTime] = useState<number>(0);

  // Phase 8 & 9 State (Likes & Nested Replies)
  const [replyingTo, setReplyingTo] = useState<{ id: string, reviewId: string, parentId: string | null } | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const [isReplying, setIsReplying] = useState(false);
  const [expandedThreads, setExpandedThreads] = useState<string[]>([]);

  const toggleThread = (id: string) => {
    setExpandedThreads(prev => 
      prev.includes(id) ? prev.filter(tid => tid !== id) : [...prev, id]
    );
  };

  useEffect(() => {
    const fetchAuthAndData = async () => {
      const { data: { session } } = await supabase.auth.getSession();
      
      if (session?.user) {
        setUser(session.user);
        
        const { data: profileData } = await supabase
          .from('profiles')
          .select('*')
          .eq('id', session.user.id)
          .single();
          
        if (profileData) {
          setProfile(profileData);
          if (!profileData.display_name) {
            setNeedsOnboarding(true);
          }
        } else {
          setNeedsOnboarding(true);
        }
      }
      
      // Fetch Banned Words
      const { data: bannedWordsData } = await supabase.from('banned_words').select('word');
      if (bannedWordsData) {
        setBannedWordsList(bannedWordsData.map((bw: any) => bw.word.toLowerCase()));
      }

      setIsLoading(false);
    };
    
    fetchAuthAndData();
  }, []);

  useEffect(() => {
    fetchReviews(selectedProduct);
  }, [selectedProduct]);

  const fetchReviews = async (product: string) => {
    const { data: reviewsData } = await supabase
      .from('reviews')
      .select(`
        id, content, created_at, user_id,
        profiles ( display_name, avatar_url, is_admin, is_verified ),
        review_likes ( user_id ),
        review_replies ( id, content, created_at, user_id, parent_id, profiles ( display_name, avatar_url, is_admin, is_verified ), reply_likes ( user_id ) )
      `)
      .eq('product', product)
      .order('created_at', { ascending: false });
      
    if (reviewsData) {
      setReviewsList(reviewsData);
    }
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!displayName) return;
    
    setIsSaving(true);
    const { error } = await supabase
      .from('profiles')
      .upsert({ 
        id: user.id, 
        display_name: displayName,
        updated_at: new Date().toISOString()
      });
      
    if (!error) {
      setProfile({ ...profile, display_name: displayName });
      setNeedsOnboarding(false);
    }
    setIsSaving(false);
  };

  const handlePostReview = async () => {
    if (!reviewContent.trim()) return;
    
    // Cooldown check (60 seconds)
    const now = Date.now();
    if (now - lastPostTime < 60000) {
      const remainingSeconds = Math.ceil((60000 - (now - lastPostTime)) / 1000);
      setPostError(`Please wait ${remainingSeconds} seconds before posting again.`);
      return;
    }
    
    setPostError('');
    setIsPosting(true);

    // Fetch latest banned words to ensure real-time moderation
    const { data: latestBannedData } = await supabase.from('banned_words').select('word');
    // Fallback to the cached list if the live fetch fails (e.g., network timeout)
    const currentBannedWords = latestBannedData ? latestBannedData.map(bw => bw.word.toLowerCase()) : bannedWordsList;

    // Banned Words/Phrases Check
    // Normalize multiple spaces into a single space so "word  word" becomes "word word"
    const lowerContent = reviewContent.toLowerCase().replace(/\s+/g, ' ');
    const escapeRegExp = (string: string) => string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    
    const containsBannedWord = currentBannedWords.some((word) => {
      // Remove accidental quotation marks from the admin dashboard input just in case
      const cleanWord = word.replace(/["']/g, '').trim().replace(/\s+/g, ' ');
      if (!cleanWord) return false;
      
      const regex = new RegExp(`(^|[^a-z0-9])${escapeRegExp(cleanWord)}([^a-z0-9]|$)`, 'i');
      return regex.test(lowerContent);
    });

    if (containsBannedWord) {
      setPostError("Your review contains inappropriate language and cannot be posted.");
      setIsPosting(false);
      return;
    }

    const { error } = await supabase
      .from('reviews')
      .insert({
        user_id: user.id,
        product: selectedProduct,
        content: reviewContent.trim(),
        rating: 5
      });

    if (error) {
      setPostError("Failed to post review: " + error.message);
    } else {
      setReviewContent('');
      setLastPostTime(Date.now()); // Update cooldown
      await fetchReviews(selectedProduct); // Refresh feed
    }
    
    setIsPosting(false);
  };

  const handleDeleteReview = async (reviewId: string) => {
    if (!confirm('Are you sure you want to delete this review?')) return;
    
    await supabase.from('reviews').delete().eq('id', reviewId);
    setReviewsList(reviewsList.filter(r => r.id !== reviewId));
  };

  const handleToggleLike = async (reviewId: string, hasLiked: boolean) => {
    if (!user) return; // Must be logged in

    // Optimistic update
    const updatedReviews = reviewsList.map(review => {
      if (review.id === reviewId) {
        if (hasLiked) {
          return { ...review, review_likes: review.review_likes.filter((l: any) => l.user_id !== user.id) };
        } else {
          return { ...review, review_likes: [...(review.review_likes || []), { user_id: user.id }] };
        }
      }
      return review;
    });
    setReviewsList(updatedReviews);

    if (hasLiked) {
      await supabase.from('review_likes').delete().match({ review_id: reviewId, user_id: user.id });
    } else {
      await supabase.from('review_likes').insert({ review_id: reviewId, user_id: user.id });
    }
  };

  const handleToggleReplyLike = async (replyId: string, hasLiked: boolean, reviewId: string) => {
    if (!user) return; // Must be logged in

    // Optimistic update
    const updatedReviews = reviewsList.map(review => {
      if (review.id === reviewId) {
        const updatedReplies = (review.review_replies || []).map((reply: any) => {
          if (reply.id === replyId) {
            if (hasLiked) {
              return { ...reply, reply_likes: reply.reply_likes.filter((l: any) => l.user_id !== user.id) };
            } else {
              return { ...reply, reply_likes: [...(reply.reply_likes || []), { user_id: user.id }] };
            }
          }
          return reply;
        });
        return { ...review, review_replies: updatedReplies };
      }
      return review;
    });
    setReviewsList(updatedReviews);

    if (hasLiked) {
      await supabase.from('reply_likes').delete().match({ reply_id: replyId, user_id: user.id });
    } else {
      await supabase.from('reply_likes').insert({ reply_id: replyId, user_id: user.id });
    }
  };

  const handlePostReply = async (reviewId: string, parentId: string | null = null) => {
    if (!replyContent.trim() || !user) return;
    
    // Cooldown check (60 seconds)
    const now = Date.now();
    if (now - lastPostTime < 60000) {
      const remainingSeconds = Math.ceil((60000 - (now - lastPostTime)) / 1000);
      alert(`Please wait ${remainingSeconds} seconds before posting again.`);
      return;
    }
    
    setIsReplying(true);

    const { data: latestBannedData } = await supabase.from('banned_words').select('word');
    const currentBannedWords = latestBannedData ? latestBannedData.map(bw => bw.word.toLowerCase()) : bannedWordsList;

    const lowerContent = replyContent.toLowerCase().replace(/\s+/g, ' ');
    const escapeRegExp = (string: string) => string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const containsBannedWord = currentBannedWords.some((word) => {
      const cleanWord = word.replace(/["']/g, '').trim().replace(/\s+/g, ' ');
      if (!cleanWord) return false;
      const regex = new RegExp(`(^|[^a-z0-9])${escapeRegExp(cleanWord)}([^a-z0-9]|$)`, 'i');
      return regex.test(lowerContent);
    });

    if (containsBannedWord) {
      alert("Your reply contains inappropriate language.");
      setIsReplying(false);
      return;
    }

    const { error } = await supabase.from('review_replies').insert({
      review_id: reviewId,
      user_id: user.id,
      parent_id: parentId,
      content: replyContent.trim()
    });

    if (!error) {
      setReplyContent('');
      setReplyingTo(null);
      setLastPostTime(Date.now()); // Update cooldown
      await fetchReviews(selectedProduct);
    } else {
      alert(error.message);
    }
    setIsReplying(false);
  };

  const handleDeleteReply = async (replyId: string) => {
    if (!confirm('Are you sure you want to delete this reply?')) return;
    await supabase.from('review_replies').delete().eq('id', replyId);
    await fetchReviews(selectedProduct);
  };

  const renderReplies = (replies: any[], parentId: string | null, reviewId: string) => {
    const currentLevelReplies = replies.filter(r => r.parent_id === parentId);
    if (currentLevelReplies.length === 0) return null;

    return currentLevelReplies.sort((a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime()).map(reply => {
      const replyAuthor = reply.profiles || {};
      const likes = reply.reply_likes || [];
      const hasLiked = likes.some((l: any) => l.user_id === user?.id);
      return (
        <div key={reply.id} className={parentId ? styles.nestedReply : styles.replyItem}>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <div style={{ flexShrink: 0 }}>
              {replyAuthor.avatar_url ? (
                <img src={replyAuthor.avatar_url} alt="Avatar" className={styles.reviewAvatar} style={{width: 32, height: 32}} />
              ) : (
                <div className={styles.reviewAvatar} style={{width: 32, height: 32, fontSize: '0.9rem'}}>
                  {(replyAuthor.display_name || 'U').charAt(0).toUpperCase()}
                </div>
              )}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className={styles.reviewHeader}>
                <div className={styles.reviewAuthor}>
                  <span style={{ fontSize: '0.9rem' }}>{replyAuthor.display_name || 'Anonymous User'}</span>
                  {replyAuthor.is_verified && (
                    <svg viewBox="0 0 24 24" fill="var(--accent-blue)" style={{ width: '14px', height: '14px', marginLeft: '-2px' }}>
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                    </svg>
                  )}
                  {replyAuthor.is_admin && (
                    <span style={{ 
                      fontSize: '0.6rem', 
                      fontWeight: 800,
                      letterSpacing: '0.05em',
                      backgroundColor: 'rgba(10, 132, 255, 0.15)', 
                      color: 'var(--accent-blue)', 
                      border: '1px solid rgba(10, 132, 255, 0.3)',
                      padding: '2px 8px', 
                      borderRadius: '12px', 
                      marginLeft: '6px',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '3px',
                      transform: 'translateY(-1px)'
                    }}>
                      <svg xmlns="http://www.w3.org/2000/svg" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                      ADMIN
                    </span>
                  )}
                  <span className={styles.reviewDate} style={{ marginLeft: '0.5rem' }}>
                    {new Date(reply.created_at).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                  </span>
                </div>
                {(user?.id === reply.user_id || profile?.is_admin) && (
                  <button onClick={() => handleDeleteReply(reply.id)} className={styles.deleteBtn} style={{ padding: '4px', display: 'flex', alignItems: 'center' }} title="Delete reply">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                  </button>
                )}
              </div>
              <div className={styles.reviewContent} style={{ fontSize: '0.9rem' }}>
                {reply.content}
              </div>
          
          <div style={{ marginTop: '0.5rem', display: 'flex', gap: '1rem', alignItems: 'center' }}>
            <button 
              className={`${styles.actionBtn} ${hasLiked ? styles.liked : ''}`} 
              style={{ padding: '0', fontSize: '0.8rem' }}
              onClick={() => handleToggleReplyLike(reply.id, hasLiked, reviewId)}
              disabled={!user}
            >
              <svg xmlns="http://www.w3.org/2000/svg" className={styles.actionIcon} style={{ width: '14px', height: '14px' }} viewBox="0 0 24 24" fill={hasLiked ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
              {likes.length}
            </button>
            <button 
              className={styles.actionBtn} 
              style={{ padding: '0', fontSize: '0.8rem' }}
              onClick={() => user ? setReplyingTo(replyingTo?.id === reply.id ? null : { id: reply.id, reviewId, parentId: reply.id }) : null}
            >
              Reply
            </button>
          </div>

          {replyingTo?.id === reply.id && (
            <div className={styles.replyCompose} style={{ marginTop: '0.75rem' }}>
              <textarea 
                className={styles.replyInput}
                placeholder="Write a reply..."
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                autoFocus
              />
              <div className={styles.replyActions}>
                <button className="btn-secondary" onClick={() => { setReplyingTo(null); setReplyContent(''); }} disabled={isReplying}>Cancel</button>
                <button className="btn-primary" onClick={() => handlePostReply(reviewId, reply.id)} disabled={isReplying || !replyContent.trim()}>
                  {isReplying ? 'Posting...' : 'Post Reply'}
                </button>
              </div>
            </div>
          )}

          {(() => {
            const childReplies = replies.filter(r => r.parent_id === reply.id);
            if (childReplies.length > 0) {
              const isExpanded = expandedThreads.includes(reply.id);
              return (
                <div style={{ marginTop: '0.75rem' }}>
                  <button 
                    className={styles.viewRepliesBtn} 
                    onClick={() => toggleThread(reply.id)}
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ transform: isExpanded ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }}>
                      <path d="m9 18 6-6-6-6"/>
                    </svg>
                    {isExpanded ? 'Hide' : 'View'} {childReplies.length} {childReplies.length === 1 ? 'reply' : 'replies'}
                  </button>
                  {isExpanded && (
                    <div className={styles.inlineSlideIn}>
                      {renderReplies(replies, reply.id, reviewId)}
                    </div>
                  )}
                </div>
              );
            }
            return null;
          })()}
            </div>
          </div>
        </div>
      );
    });
  };

  return (
    <main className={styles.main}>
      
      <div className={styles.container}>
        <div className={styles.heroSection}>
          <h1 className={styles.title}>Community Reviews</h1>
          <p className={styles.subtitle}>Read what other users are saying about our software, and share your own experiences.</p>
        </div>

        {isLoading ? (
          <div className={styles.loading}>Loading community feed...</div>
        ) : needsOnboarding ? (
          <div className={styles.onboardingCard}>
            <h2>Welcome to the Community!</h2>
            <p>Before you can view or post reviews, please set a public display name. This protects your privacy.</p>
            <form onSubmit={handleSaveProfile} className={styles.onboardingForm}>
              <input 
                type="text" 
                value={displayName} 
                onChange={(e) => setDisplayName(e.target.value)} 
                placeholder="e.g. RetailKing99"
                required
                maxLength={30}
                className={styles.composeInput}
                style={{ width: '100%', marginBottom: '1rem' }}
              />
              <button type="submit" className="btn-primary" disabled={isSaving}>
                {isSaving ? 'Saving...' : 'Set Display Name & Continue'}
              </button>
            </form>
          </div>
        ) : (
          <div className={styles.feedLayout}>
            {/* Left Sidebar */}
            <aside className={styles.sidebar}>
              <div className={styles.card}>
                <h3>Select Product</h3>
                <ul className={styles.productList}>
                  <li 
                    className={selectedProduct === 'ShopDesk' ? styles.activeProduct : styles.inactiveProduct}
                    onClick={() => setSelectedProduct('ShopDesk')}
                  >
                    ShopDesk
                  </li>
                  <li 
                    className={selectedProduct === 'Craftag' ? styles.activeProduct : styles.inactiveProduct}
                    onClick={() => setSelectedProduct('Craftag')}
                  >
                    Craftag
                  </li>
                  <li 
                    className={selectedProduct === 'PMS' ? styles.activeProduct : styles.inactiveProduct}
                    onClick={() => setSelectedProduct('PMS')}
                  >
                    PMS
                  </li>
                  <li 
                    className={selectedProduct === 'LMS' ? styles.activeProduct : styles.inactiveProduct}
                    onClick={() => setSelectedProduct('LMS')}
                  >
                    LMS
                  </li>

                </ul>
              </div>
            </aside>

            {/* Main Content */}
            <div className={styles.mainFeed}>
              {user ? (
                <div className={styles.composeBox}>
                  <div className={styles.composeHeader}>
                    {profile?.avatar_url ? (
                      <img 
                        src={profile.avatar_url} 
                        alt="Your Avatar" 
                        className={styles.composeAvatar} 
                        style={{ objectFit: 'cover', border: 'none', padding: 0 }} 
                      />
                    ) : (
                      <div className={styles.composeAvatar}>
                        {profile?.display_name ? profile.display_name.charAt(0).toUpperCase() : user?.user_metadata?.display_name?.charAt(0).toUpperCase() || 'U'}
                      </div>
                    )}
                    <textarea 
                      className={styles.composeInput}
                      placeholder={`Write a review for ${selectedProduct}...`}
                      rows={3}
                      value={reviewContent}
                      onChange={(e) => setReviewContent(e.target.value)}
                    />
                  </div>
                  {postError && <div className={styles.errorText}>{postError}</div>}
                  <div className={styles.composeActions}>
                    <button 
                      className="btn-primary" 
                      disabled={isPosting || !reviewContent.trim()}
                      onClick={handlePostReview}
                    >
                      {isPosting ? 'Posting...' : 'Post Review'}
                    </button>
                  </div>
                </div>
              ) : (
                <div className={styles.authPrompt}>
                  <p>You must be signed in to leave a review.</p>
                </div>
              )}

              <div className={styles.reviewsList}>
                {reviewsList.length === 0 ? (
                  <div className={styles.emptyState}>
                    <span className={styles.emptyIcon}>💬</span>
                    <p>No reviews yet. Be the first to share your thoughts!</p>
                  </div>
                ) : (
                  reviewsList.map((review) => {
                    const authorProfile = review.profiles || {};
                    const likes = review.review_likes || [];
                    const hasLiked = likes.some((l: any) => l.user_id === user?.id);
                    const replies = review.review_replies || [];
                    return (
                      <div key={review.id} className={styles.reviewItem}>
                        <div style={{ display: 'flex', gap: '1rem' }}>
                          <div style={{ flexShrink: 0 }}>
                            {authorProfile.avatar_url ? (
                              <img src={authorProfile.avatar_url} alt="Avatar" className={styles.reviewAvatar} style={{ width: 40, height: 40 }} />
                            ) : (
                              <div className={styles.reviewAvatar} style={{ width: 40, height: 40, fontSize: '1.2rem' }}>
                                {(authorProfile.display_name || 'U').charAt(0).toUpperCase()}
                              </div>
                            )}
                          </div>
                          
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div className={styles.reviewHeader}>
                              <div className={styles.reviewAuthor}>
                                <span>{authorProfile.display_name || 'Anonymous User'}</span>
                                {authorProfile.is_verified && (
                                  <svg viewBox="0 0 24 24" fill="var(--accent-blue)" style={{ width: '16px', height: '16px', marginLeft: '-2px' }}>
                                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                                  </svg>
                                )}
                                {authorProfile.is_admin && (
                                  <span style={{ 
                                    fontSize: '0.65rem', 
                                    fontWeight: 800,
                                    letterSpacing: '0.05em',
                                    backgroundColor: 'rgba(10, 132, 255, 0.15)', 
                                    color: 'var(--accent-blue)', 
                                    border: '1px solid rgba(10, 132, 255, 0.3)',
                                    padding: '2px 8px', 
                                    borderRadius: '12px', 
                                    marginLeft: '6px',
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '3px',
                                    transform: 'translateY(-1px)'
                                  }}>
                                    <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                                    ADMIN
                                  </span>
                                )}
                                <span className={styles.reviewDate} style={{ marginLeft: '0.75rem' }}>
                                  {new Date(review.created_at).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                                </span>
                              </div>
                              {(user?.id === review.user_id || profile?.is_admin) && (
                                <button onClick={() => handleDeleteReview(review.id)} className={styles.deleteBtn} style={{ padding: '4px', display: 'flex', alignItems: 'center' }} title="Delete review">
                                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                                </button>
                              )}
                            </div>
                            
                            <div className={styles.reviewContent}>
                              {review.content}
                            </div>
                        
                        <div className={styles.actionBar}>
                          <button 
                            className={`${styles.actionBtn} ${hasLiked ? styles.liked : ''}`} 
                            onClick={() => handleToggleLike(review.id, hasLiked)}
                            disabled={!user}
                          >
                            <svg xmlns="http://www.w3.org/2000/svg" className={styles.actionIcon} viewBox="0 0 24 24" fill={hasLiked ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                            </svg>
                            {likes.length}
                          </button>
                          <button 
                            className={styles.actionBtn}
                            onClick={() => user ? setReplyingTo(replyingTo?.id === review.id ? null : { id: review.id, reviewId: review.id, parentId: null }) : null}
                          >
                            <svg xmlns="http://www.w3.org/2000/svg" className={styles.actionIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                            </svg>
                            {replies.length > 0 ? replies.length : 'Reply'}
                          </button>
                        </div>

                        {(() => {
                          const directReplies = replies.filter((r: any) => r.parent_id === null);
                          if (directReplies.length > 0) {
                            const isExpanded = expandedThreads.includes(review.id);
                            return (
                              <div style={{ marginTop: '0.75rem' }}>
                                <button 
                                  className={styles.viewRepliesBtn} 
                                  onClick={() => toggleThread(review.id)}
                                >
                                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ transform: isExpanded ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }}>
                                    <path d="m9 18 6-6-6-6"/>
                                  </svg>
                                  {isExpanded ? 'Hide' : 'View'} {directReplies.length} {directReplies.length === 1 ? 'reply' : 'replies'}
                                </button>
                                {isExpanded && (
                                  <div className={styles.inlineSlideIn} style={{ marginTop: '1rem' }}>
                                    {renderReplies(replies, null, review.id)}
                                  </div>
                                )}
                              </div>
                            );
                          }
                          return null;
                        })()}

                        {replyingTo?.id === review.id && (
                          <div className={styles.repliesContainer} style={{ borderLeft: 'none', paddingLeft: 0 }}>
                            <div className={styles.replyCompose}>
                              <textarea 
                                className={styles.replyInput}
                                placeholder="Write a reply..."
                                value={replyContent}
                                onChange={(e) => setReplyContent(e.target.value)}
                                autoFocus
                              />
                              <div className={styles.replyActions}>
                                <button className="btn-secondary" onClick={() => { setReplyingTo(null); setReplyContent(''); }} disabled={isReplying}>Cancel</button>
                                <button className="btn-primary" onClick={() => handlePostReply(review.id)} disabled={isReplying || !replyContent.trim()}>
                                  {isReplying ? 'Posting...' : 'Post Reply'}
                                </button>
                              </div>
                            </div>
                          </div>
                        )}

                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>
        )}
      </div>

    </main>
  );
}
