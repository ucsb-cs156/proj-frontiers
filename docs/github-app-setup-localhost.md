# Setting up frontiers as a Github App on Localhost

You should already have a `.env` file that you created by copying from `.env.SAMPLE`.

Start by following the instructions in [`docs/oauth.md`] to obtain values for these
variables and setting them in `.env`
* `GOOGLE_CLIENT_ID`
* `GOOGLE_CLIENT_SECRET`

These instructions will explain how to fill in the values for:
* `GITHUB_CLIENT_ID`
* `GITHUB_CLIENT_SECRET`
* `app_private_key`

## Obtaining `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET`

Go to <https://github.com/>

Then, click your profile icon. Click "Settings". Then, Click "Developer Settings", on the bottom of the toolbar on the left.

Select "New Github App". Fill in an appropriate name, and write it down. You will need it later.

![image](https://github.com/user-attachments/assets/3d0fe501-318c-4907-a267-eff44f06f17a)

For the homepage url, fill in `http://localhost:8080`.

![image](https://github.com/user-attachments/assets/bec66087-ca4a-4fc4-af3d-9ad663c24eb2)

Under `Identifying and authorizing users`, click the button "Add Callback URL"; this should make it so that you see two spaces in which you can add a Callback URL.

Fill these in as follows:
* First callback URL: `http://localhost:8080/api/courses/link`
* Second callback URL: `http://localhost:8080/login/oauth2/code/github`

Click the checkbox for "Request user authorization (OAuth) during installation"

![image](https://github.com/user-attachments/assets/a06af72c-e08d-4f47-bd6a-91b5d1aef65f)

Scroll down to permissions, and under repository, set the following accesses:
- Administration: Read and Write
- Contents: Read and Write
- Metadata: Read-only
- Workflows: Read and Write

Under Organization, select the following permissions:
- Administration: Read and Write

Then, scroll further and uncheck "Active" under "Webhooks"

![image](https://github.com/user-attachments/assets/74119317-b1a5-40c8-88ce-d7e394f7e5a6)


Then, scroll further and under "Where can this Github App be installed?" select "Any Account"

Click "Create".

Now, Select "Generate Client Secret". Copy this client secret to a safe location, you will use it in a few minutes. Copy the Client ID as well.

![image](https://github.com/user-attachments/assets/856cf882-b6f3-44a5-b70b-115531bb8cae)

* Copy the value for the client id into `GITHUB_CLIENT_ID` in `.env`
* Copy the value for the client secret into `GITHUB_CLIENT_SECRET` in `.env`

## Generating a value for `app_private_key`

Scroll down to "Private Keys" and select "Generate a Private Key"

![image](https://github.com/user-attachments/assets/7c2b958a-f912-4972-af63-9ff2c30339cd)


Next, we will run a script that converts this private key 
into a `dokku config:set ...` command.

Copy the key from wherever it downloaded to the *root of the frontiers project*, i.e. the directory where you cloned the repo.

For example, if `~/Downloads` is the directory where files are downloaded, then this command will copy all files ending in `.private-key.pem` to your current directory.

```
cp ~/Downloads/*.private-key.pem .
```

Note that you *must not commit* this private key to the Github Repo! The `.gitignore` should handle this, but be careful in any case.

Now run this script: 
```
./keyconvert-localhost.sh
```

* If there is only one file ending in `private-key.pem` in the current directory, it will be selected automatically. Otherwise, you'll be asked to choose one.

The script will then output a file called `secrets.yaml`


```yaml
app:
  private:
    key: "-----BEGIN PRIVATE KEY-----
<Your Key>
-----END PRIVATE KEY-----
"
```

You should now be able to run the app using:

```bash
mvn spring-boot:run
```
