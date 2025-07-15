#!/bin/bash

# Function to validate valid dokku app name
validate_app_name() {
  local app_name="$1"
  if [[ -z "$app_name" || ! "$app_name" =~ ^[a-z0-9-]+$ ]]; then
    echo "Invalid app name. Please enter a valid Dokku app name (lowercase letters, numbers, and hyphens only)."
    return 1
  fi
  return 0
}

# Function to valid if the provided PEM file is a valid RSA key
validate_pem_file() {
  local pem_file="$1"
  if [[ ! -f "$pem_file" ]]; then
    echo "File $pem_file does not exist."
    return 1
  fi
  if [[ ! "$1" =~ "\.private-key\.pem$" ]]; then
    echo "File $pem_file is not a valid PEM file. Filename should end with .private-key.pem."
    return 1
  fi
  if ! openssl rsa -in "$pem_file" -noout -check > /dev/null 2>&1; then
    echo "File $pem_file is not a valid RSA key."
    return 1
  fi
  return 0 
}

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
    echo "No PEM file found in the current directory."
    echo "Please download a private key from the GitHub app settings page,"
    echo "and save it in the current directory."
    exit 1 
fi

if openssl rsa -in $pem_file --noout -check > /dev/null 2>&1; then
  openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in $pem_file -out pkcs8.key
  echo "successfully converted key"
else
  echo "File $1 not a valid rsa key"
fi

echo "Please provide the Dokku app name to set the private key for:"
echo "For example, frontiers, frontiers-qa, frontiers-cgaucho, etc"
echo ""
read -p "Enter the Dokku app name: " app_name
# Validate the app name with a loop until a valid name is provided
while ! validate_app_name "$app_name"; do
echo "Invalid app name. Please enter a valid Dokku app name (lowercase letters, numbers, and hyphens only):"
read -p "Enter the Dokku app name: " app_name
done


echo "Copy/paste the following command to set the private key for your Dokku app:"
echo ""
echo "dokku config:set --no-restart $app_name app_private_key=\"$(cat pkcs8.key)\""

