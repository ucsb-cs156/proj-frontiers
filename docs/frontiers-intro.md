## Intro To Frontiers

This project is a replacement for the old github-linker used by UCSB's Computer Science Department.

This app manipulates the GitHub Rest API to create student repositories and manage courses for instructors.

## Crash Course to GitHub Rest API:
The GitHub Rest API can be accessed by a few types of applications:
- Applications acting **on behalf** of a user
- Applications as themselves
 
The main difference between the two is that some endpoints are restricted to users only. However, both can access most endpoints, and the only major difference is that the changes are attributed to an application rather than a user.

GitHub Rest endpoints are secured one of two ways: by a JWT (JavaScript Web Token) or a GitHub token.
Endpoints that are for an entire application, like seeing where it is installed, or changing application settings, or obtaining a GitHub token, are secured by a JWT. GitHub provides a private key that can be used to sign a JWT to prove to GitHub you are who you say you are. 

GitHub Tokens, on the other hand, are used to act as a specific "installation" of an application. They are restricted in permissions to whatever "installation" you act as -- ie, you can be "ucsb-cs156" installation of Frontiers that has access to the ucsb-cs156 organization's repositories, member list, and more. However, this specific application does not have access to a different organization's repositories, like "ucsb-cs156-s25". You would have to act as the ucsb-cs156-s25 installation to gain access to those.

There are abstracted in frontiers via the `JwtService`, which provides access to JWTs via `getJwt()` and installation tokens via `getInstallationToken(String installationId)`. All that is needed to access one of these endpoints is to provide whichever token is listed in the Reference in the `Authorization` header, listed as `Bearer: <token>`

## Frontiers-Specific Applications:
In frontiers, every installation is linked to a Course instance. This is so that we know the GitHub organization that a particular course is using. The `installationId` property of a Course can be used to obtain a GitHub installation token for a particular installation, and the `organization` property can be used to get the organization name of a particular organization.

Together, these can usually be used to access any endpoint that is listed in the GitHub Rest API reference. 

However, endpoints that require user accounts cannot be accessed - Frontiers will prevent installation to a user account rather than an organization.