# Setting up the Github App for Frontiers running on Dokku

Go to https://github.com/

Then, click your profile icon. Click "Settings". Then, Click "Developer Settings", on the bottom of the toolbar on the left.

Select "New Github App". Fill in an appropriate name, and write it down. You will need it later.

![image](https://github.com/user-attachments/assets/3d0fe501-318c-4907-a267-eff44f06f17a)


For the homepage url, fill in `https://<appname>.dokku-<dokku-number>.cs.ucsb.edu`.

![image](https://github.com/user-attachments/assets/c0e06e2a-2aad-4b3a-af55-46448ed571ee)



Next to the heading for `Identifying and authorizing users`, find the button "Add Callback URL", and click it once.

You should now see two spaces to add Callback URLs, like this:

<img width="878" alt="image" src="https://github.com/user-attachments/assets/1f3bcf9b-113e-4d72-bc0f-f5687b63e172" />


Fill in the two callback URLs with these values:
```
https://<appname>.dokku-<dokku-number>.cs.ucsb.edu/api/courses/link
https://<appname>.dokku-<dokku-number>.cs.ucsb.edu/login/oauth2/code/github
```
Replacing `<appname>` with the name of your app, and `<dokku-number>` with your dokku installation.

Then, click the checkbox for "Request user authorization (OAuth) during installation"

As an example, when filled it, it might look like this:

<img width="898" alt="image" src="https://github.com/user-attachments/assets/ee58c776-0e1d-4290-b278-8ea4d87884f0" />


Scroll further and under webhooks, fill in the following url where *appname* is your appname and *xx* is your dokku server:
```
https://appname.dokku-xx.cs.ucsb.edu/api/webhooks/github
```

Scroll down to permissions, and under repository, set the following accesses:
- Administration: Read and Write
- Contents: Read and Write
- Metadata: Read-only
- Workflows: Read and Write

Under Organization, select the following permissions:
- Administration: Read and Write
- Members: Read and Write


![image](https://github.com/user-attachments/assets/5ba94bdb-d4ce-4911-a80f-248e8e231a24)

Then, scroll further and under "Subscribe to Events" select "Organization"

![image](https://github.com/user-attachments/assets/65491ad0-ef2b-4542-891b-852365f2366b)


Then, scroll further and under "Where can this Github App be installed?" select "Any Account"

Click "Create".

Now, Select "Generate Client Secret". 

![image](https://github.com/user-attachments/assets/856cf882-b6f3-44a5-b70b-115531bb8cae)


Then, set your Github Client ID and Github Client Secret with the following commands respectively:
```bash
dokku config:set --no-restart <appname> GITHUB_CLIENT_ID=<client-id>
dokku config:set --no-restart <appname> GITHUB_CLIENT_SECRET=<client-secret>
```

## Generating a value for `app_private_key`

Scroll down to "Private Keys" and select "Generate a Private Key"

![image](https://github.com/user-attachments/assets/7c2b958a-f912-4972-af63-9ff2c30339cd)


This will download a private key file (with file name ending `.private-key.pem` to your computer, probably into your default `Downloads` directory.  We'll need this file in the next step.

Note that the file has the current date in the filename; this will help you be sure you have the correct file.  We'll use this file in the next step.

## Converting the private key file.

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
./keyconvert.sh
```

* If there is only one file ending in `private-key.pem` in the current directory, it will be selected automatically. Otherwise, you'll be asked to choose one.
* The script will then prompt you for the name of your dokku app (e.g. `frontiers`, `frontiers-qa`, etc.)

The script will then output the command that you should copy to the dokku command line

As this is a multiline variable, it will need to be between a set of quotes. Make sure that you paste it between the quotes, otherwise the variable will only be set to the first line of the key. Use the following command, replacing <appname> with your app name and <file-output> with the copied output from the previous step:
```bash
dokku config:set frontiers --no-restart app_private_key="-----BEGIN PRIVATE KEY-----
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
xxxxxxxxxxxxxxxxxxxxxxxxx
-----END PRIVATE KEY-----"
```

## Other steps you may need to do

If you haven't yet done so, do the other steps listed in [/docs/dokku.md](dokku.md).

