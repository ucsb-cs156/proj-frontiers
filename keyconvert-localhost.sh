#!/bin/bash

# If there is more than one .pem file in the current directory, prompt the user to select one
pem_files=($(find . -maxdepth 1 -type f -name "*.private-key.pem"))
if [ ${#pem_files[@]} -gt 1 ]; then
  echo "Multiple PEM files found. Please select one:"
  select pem_file in "${pem_files[@]}"; do
    if [[ -n "$pem_file" ]]; then
      break
    else
      echo "Invalid selection. Please try again."
    fi
  done
elif [ ${#pem_files[@]} -eq 0 ]; then
  echo "No PEM files found. Please provide a valid PEM file"
  exit 1
else 
  pem_file=$(find . -maxdepth 1 -type f -name "*.pem" | head -n 1)
fi

if [ -z "$pem_file" ]; then
    echo "No PEM file found. Please provide a valid PEM file."
    exit 1 
fi

if openssl rsa -in $pem_file --noout -check > /dev/null 2>&1; then
  openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in $pem_file -out pkcs8.key
  echo "successfully converted key"
else
  echo "File $1 not a valid rsa key"
fi

# Read the contents of pkcs8.key and escape for YAML (indent each line by two spaces)
key_content=$(awk '{print "  " $0}' pkcs8.key)

# If secrets.yaml already exists, prompt the user to confirm overwriting
# and back it up if they choose to overwrite
if [ -f secrets.yaml ]; then
  echo "secrets.yaml already exists. Do you want to overwrite it? (y/n)"
  read -r confirm
  if [[ "$confirm" != "y" ]]; then
    echo "Exiting without overwriting secrets.yaml."
    exit 0
  fi
  mv secrets.yaml secrets.yaml.backup
  echo "Existing secrets.yaml backed up as secrets.yaml.backup"
fi

# Write the contents to secrets.yaml
cat <<EOF > secrets.yaml
app:
  private:
    key: "-----BEGIN PRIVATE KEY-----
$key_content
-----END PRIVATE KEY-----"
EOF

echo "secrets.yaml has been created with the converted key."
