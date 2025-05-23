if [ -f "$1" ]; then
if openssl rsa -in $1 --noout -check > /dev/null 2>&1; then
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in $1 -out pkcs8.key
echo "successfully converted key"
else
echo "not a valid rsa key"
fi
else
echo "$1 does not exist"
fi
