**If you are a student in CS156, this is not required of you. This is not part of the course, or a part of Frontiers you will be required to work on.**

## Azure Active Directory
Azure Active Directory (also known as Entra ID) is a service offered by Azure that allows users to sign on with their Active Directory accounts. These accounts reside in an Active Directory "tenant". 
This stores of all their account information, and is identified by a Tenant ID.

In order to use Active Directory as a sign-on, a couple steps need to be taken:
# Setup
Sign up for an Azure account.

This may require a credit card, as Azure will use it to identify that you are a real person, and will reverse the charge shortly after.

Currently, it is not possible to use your UCSB Account.

From the hamburger menu on the left, Select Entra ID

<img width="50%" height="50%" alt="image" src="https://github.com/user-attachments/assets/34fcd148-8b1c-46eb-a11f-24cf9b932165" />

Select App Registrations

<img width="50%" height="50%" alt="image" src="https://github.com/user-attachments/assets/20f99256-e82e-42ae-af17-a760dfbb09e2" />

Select New Registration

<img width="1000" height="435" alt="image" src="https://github.com/user-attachments/assets/f8ae7a65-c0e8-4d69-86b1-d9a6fef18a8f" />

Fill in a name, and select "Accounts in any organizational directory (Any Microsoft Entra ID tenant - Multitenant)"

<img width="1378" height="976" alt="image" src="https://github.com/user-attachments/assets/9fb52c95-b655-4b1f-9a3a-425adb919c04" />

Then, fill in a Redirect URI:
```web
http://localhost:8080/login/oauth2/code/azure-dev
```
Select web as the platform.

Click create.

From this next screen, copy your Application ID and Directory (tenant) ID to their places in .env respectively:
```env
MICROSOFT_TENANT_ID=<tenant-id>
MICROSOFT_CLIENT_ID=<client-id>
```

Next, let's add a client secret. Select "Add a certificate or secret", then select "New Client Secret".

Give it a name and an expiry date. Click add, and copy the value to its respective place in .env:
```env
MICROSOFT_CLIENT_SECRET=<client-secret>
```

Finally, let's add at least one more Redirect URI. Select "Authentication"

<img width="200" height="420" alt="image" src="https://github.com/user-attachments/assets/ee62bce6-4bb8-4ba1-aa8c-407d503227b5" />

Then, from "Web" select "Add URI".

Add your dokku installation with the following URI:
```web
https://<appname>.dokku-<dokku-number>.cs.ucsb.edu/login/oauth2/code/azure-dev
```

Click "Save" at the bottom.

This is now set up. To use it on localhost, place `MICROSOFT=true` at the beginning of the spring boot run command, like so:
`MICROSOFT=true mvn spring-boot:run`.

To use on dokku, add `MICROSOFT_CLIENT_SECRET`, `MICROSOFT_TENANT_ID`, and `MICROSOFT_CLIENT_ID` as environmental variables. Additonally, you must run the following:
```bash
dokku config:set --no-restart <appname> MICROSOFT=true
```

The setup is now complete!

## Implementation Notes
When Microsoft issues OIDC tokens, they are listed with the issuer including the AD Tenant ID. Since this changes depending on the organization, other Active Directory tenants (ie Oregon State or Chico State)
have different Tenant IDs. As a result, when they try to sign in, they are listed as being issued by those tenants. Spring Security, by default, does not allow this, and so `SecurityConfig.java` includes a custom validator for the tokens
that checks the result of the url, and ensures the only difference is the tenant ID. 

Additionally, since Microsoft requires you to use your own tenant ID as the issuer url, most of the OIDC endpoints are manually set in `application-microsoft.properties`.
