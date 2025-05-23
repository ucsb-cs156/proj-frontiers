if [ -f "$1" ]; then
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in $1 -out pkcs8.key
else
echo "$1 does not exist"
fi
