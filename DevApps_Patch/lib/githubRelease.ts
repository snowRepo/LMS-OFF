type GitHubAsset = {
  name?: string;
  browser_download_url?: string;
};

type GitHubRelease = {
  tag_name?: string;
  assets?: GitHubAsset[];
};

const CRAFTAG_REPO = 'snowRepo/Craftag';
const SHOPDESK_REPO = 'snowRepo/ShopDesk-Releases';

function normalizeVersion(tagName?: string) {
  return (tagName || '').replace(/^v/, '');
}

function buildReleaseAssetUrl(tagName: string, fileName: string) {
  return `https://github.com/${CRAFTAG_REPO}/releases/download/${tagName}/${fileName}`;
}

function pickAssetUrl(assets: GitHubAsset[] | undefined, matchers: RegExp[]) {
  return assets?.find((asset) => {
    const name = asset.name?.toLowerCase() || '';
    return matchers.some((matcher) => matcher.test(name));
  })?.browser_download_url;
}

export async function getLatestCraftagRelease() {
  try {
    const response = await fetch(
      `https://api.github.com/repos/${CRAFTAG_REPO}/releases/latest`,
      {
        headers: {
          Accept: 'application/vnd.github+json',
          'User-Agent': 'DevApps-Website'
        },
        cache: 'no-store'
      }
    );

    if (!response.ok) {
      throw new Error(`GitHub release lookup failed with status ${response.status}`);
    }

    const release = (await response.json()) as GitHubRelease;
    const tagName = release.tag_name || 'v1.0.2';
    const normalizedVersion = normalizeVersion(tagName);
    const assets = release.assets || [];

    const macDownloadUrl =
      pickAssetUrl(assets, [/macos/i, /dmg/i]) ||
      buildReleaseAssetUrl(tagName, 'Craftag-macOS.dmg');

    const windowsDownloadUrl =
      pickAssetUrl(assets, [/windows/i, /exe/i]) ||
      buildReleaseAssetUrl(tagName, 'Craftag-Windows-Installer.exe');

    return {
      latestVersion: normalizedVersion,
      downloadUrl: macDownloadUrl,
      downloads: [
        {
          text: 'Download for macOS (Universal) ↓',
          link: macDownloadUrl
        },
        {
          text: 'Download for Windows ↓',
          link: windowsDownloadUrl
        }
      ]
    };
  } catch {
    const fallbackVersion = '1.0.2';

    return {
      latestVersion: fallbackVersion,
      downloadUrl: 'https://github.com/snowRepo/Craftag/releases/download/v1.0.2/Craftag-macOS.dmg',
      downloads: [
        {
          text: 'Download for macOS (Universal) ↓',
          link: 'https://github.com/snowRepo/Craftag/releases/download/v1.0.2/Craftag-macOS.dmg'
        },
        {
          text: 'Download for Windows ↓',
          link: 'https://github.com/snowRepo/Craftag/releases/download/v1.0.2/Craftag-Windows-Installer.exe'
        }
      ]
    };
  }
}

export async function getLatestShopDeskRelease() {
  try {
    const response = await fetch(
      `https://api.github.com/repos/${SHOPDESK_REPO}/releases/latest`,
      {
        headers: {
          Accept: 'application/vnd.github+json',
          'User-Agent': 'DevApps-Website'
        },
        cache: 'no-store'
      }
    );

    if (!response.ok) {
      throw new Error(`GitHub release lookup failed with status ${response.status}`);
    }

    const release = (await response.json()) as GitHubRelease;
    const tagName = release.tag_name || 'v1.0.0';
    const normalizedVersion = normalizeVersion(tagName);
    const assets = release.assets || [];

    const macDownloadUrl =
      pickAssetUrl(assets, [/macos/i, /dmg/i]) ||
      `https://github.com/${SHOPDESK_REPO}/releases/latest/download/ShopDesk_${normalizedVersion}.dmg`;

    return {
      latestVersion: normalizedVersion,
      downloadUrl: macDownloadUrl,
      downloads: [
        {
          text: 'Download for macOS (Universal) ↓',
          link: macDownloadUrl
        }
      ]
    };
  } catch {
    const fallbackVersion = '1.0.0';

    return {
      latestVersion: fallbackVersion,
      downloadUrl: `https://github.com/${SHOPDESK_REPO}/releases/latest/download/ShopDesk_1.0.0.dmg`,
      downloads: [
        {
          text: 'Download for macOS (Universal) ↓',
          link: `https://github.com/${SHOPDESK_REPO}/releases/latest/download/ShopDesk_1.0.0.dmg`
        }
      ]
    };
  }
}

const PMS_REPO = 'snowRepo/PMS';

export async function getLatestPMSRelease() {
  try {
    const response = await fetch(
      `https://api.github.com/repos/${PMS_REPO}/releases/latest`,
      {
        headers: {
          Accept: 'application/vnd.github+json',
          'User-Agent': 'DevApps-Website'
        },
        cache: 'no-store'
      }
    );

    if (!response.ok) {
      throw new Error(`GitHub release lookup failed with status ${response.status}`);
    }

    const release = (await response.json()) as GitHubRelease;
    const tagName = release.tag_name || 'v1.0.0';
    const normalizedVersion = normalizeVersion(tagName);
    const assets = release.assets || [];

    const getUrl = (includes: string[], fallback: string) => {
      const asset = assets.find(a => {
        const n = a.name?.toLowerCase() || '';
        return includes.every(i => n.includes(i));
      });
      return asset?.browser_download_url || fallback;
    };

    const macArmUrl = getUrl(['macos', 'arm', 'dmg'], `https://github.com/${PMS_REPO}/releases/latest/download/PMS-${normalizedVersion}-macos-arm64.dmg`);
    const macIntelUrl = getUrl(['macos', 'intel', 'dmg'], `https://github.com/${PMS_REPO}/releases/latest/download/PMS-${normalizedVersion}-macos-intel.dmg`);
    const winUrl = getUrl(['windows', 'msi'], `https://github.com/${PMS_REPO}/releases/latest/download/PMS-${normalizedVersion}-windows-x64.msi`);
    const linuxUrl = getUrl(['linux', 'deb'], `https://github.com/${PMS_REPO}/releases/latest/download/PMS-${normalizedVersion}-linux-x64.deb`);

    return {
      latestVersion: normalizedVersion,
      downloadUrl: macArmUrl,
      downloads: [
        { text: 'Download for macOS (Apple Silicon) ↓', link: macArmUrl },
        { text: 'Download for macOS (Intel) ↓', link: macIntelUrl },
        { text: 'Download for Windows (64-bit) ↓', link: winUrl },
        { text: 'Download for Linux ↓', link: linuxUrl }
      ]
    };
  } catch {
    const fallbackVersion = '1.0.0';

    return {
      latestVersion: fallbackVersion,
      downloadUrl: `https://github.com/${PMS_REPO}/releases/latest/download/PMS-1.0.0-macos-arm64.dmg`,
      downloads: [
        { text: 'Download for macOS (Apple Silicon) ↓', link: `https://github.com/${PMS_REPO}/releases/latest/download/PMS-1.0.0-macos-arm64.dmg` },
        { text: 'Download for macOS (Intel) ↓', link: `https://github.com/${PMS_REPO}/releases/latest/download/PMS-1.0.0-macos-intel.dmg` },
        { text: 'Download for Windows (64-bit) ↓', link: `https://github.com/${PMS_REPO}/releases/latest/download/PMS-1.0.0-windows-x64.msi` },
        { text: 'Download for Linux ↓', link: `https://github.com/${PMS_REPO}/releases/latest/download/PMS-1.0.0-linux-x64.deb` }
      ]
    };
  }
}

const LMS_REPO = 'snowRepo/LMS-OFF';

export async function getLatestLMSRelease() {
  try {
    const response = await fetch(
      `https://api.github.com/repos/${LMS_REPO}/releases/latest`,
      {
        headers: {
          Accept: 'application/vnd.github+json',
          'User-Agent': 'DevApps-Website'
        },
        cache: 'no-store'
      }
    );

    if (!response.ok) {
      throw new Error(`GitHub release lookup failed with status ${response.status}`);
    }

    const release = (await response.json()) as GitHubRelease;
    const tagName = release.tag_name || 'v1.0.0';
    const normalizedVersion = normalizeVersion(tagName);
    const assets = release.assets || [];

    const getUrl = (includes: string[], fallback: string) => {
      const asset = assets.find(a => {
        const n = a.name?.toLowerCase() || '';
        return includes.every(i => n.includes(i));
      });
      return asset?.browser_download_url || fallback;
    };

    const macArmUrl = getUrl(['macos', 'applesilicon', 'dmg'], `https://github.com/${LMS_REPO}/releases/latest/download/LMS-macOS-AppleSilicon.dmg`);
    const macIntelUrl = getUrl(['macos', 'intel', 'dmg'], `https://github.com/${LMS_REPO}/releases/latest/download/LMS-macOS-Intel.dmg`);
    const winUrl = getUrl(['windows', 'exe'], `https://github.com/${LMS_REPO}/releases/latest/download/LMS-Windows.exe`);
    const linuxUrl = getUrl(['linux', 'deb'], `https://github.com/${LMS_REPO}/releases/latest/download/LMS-Linux.deb`);

    return {
      latestVersion: normalizedVersion,
      downloadUrl: macArmUrl,
      downloads: [
        { text: 'Download for macOS (Apple Silicon) ↓', link: macArmUrl },
        { text: 'Download for macOS (Intel) ↓', link: macIntelUrl },
        { text: 'Download for Windows (64-bit) ↓', link: winUrl },
        { text: 'Download for Linux ↓', link: linuxUrl }
      ]
    };
  } catch {
    const fallbackVersion = '1.0.0';

    return {
      latestVersion: fallbackVersion,
      downloadUrl: `https://github.com/${LMS_REPO}/releases/latest/download/LMS-macOS-AppleSilicon.dmg`,
      downloads: [
        { text: 'Download for macOS (Apple Silicon) ↓', link: `https://github.com/${LMS_REPO}/releases/latest/download/LMS-macOS-AppleSilicon.dmg` },
        { text: 'Download for macOS (Intel) ↓', link: `https://github.com/${LMS_REPO}/releases/latest/download/LMS-macOS-Intel.dmg` },
        { text: 'Download for Windows (64-bit) ↓', link: `https://github.com/${LMS_REPO}/releases/latest/download/LMS-Windows.exe` },
        { text: 'Download for Linux ↓', link: `https://github.com/${LMS_REPO}/releases/latest/download/LMS-Linux.deb` }
      ]
    };
  }
}
