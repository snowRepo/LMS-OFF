import os
import re

dir_path = "src/main/java/com/lms/ui"

for root, dirs, files in os.walk(dir_path):
    for file in files:
        if file.endswith(".java"):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()

            # For Alert confirm = new Alert(...);
            # or Alert alert = new Alert(...);
            # We want to add alert.initOwner(com.lms.util.Navigator.getStage());
            
            # Using a regex to find instantiations
            new_content = re.sub(
                r'(Alert\s+(\w+)\s*=\s*new\s+Alert\([^;]+;)', 
                r'\1\n\2.initOwner(com.lms.util.Navigator.getStage());', 
                content
            )
            
            new_content = re.sub(
                r'(Dialog<\w+>\s+(\w+)\s*=\s*new\s+Dialog<>\(\);)', 
                r'\1\n\2.initOwner(com.lms.util.Navigator.getStage());', 
                new_content
            )

            # Some files might not use Navigator directly, so we use full path just in case
            if new_content != content:
                with open(filepath, 'w') as f:
                    f.write(new_content)
                print(f"Patched {filepath}")

