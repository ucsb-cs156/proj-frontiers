
# Deploying on Dokku

To deploy on dokku, see the instructions here:

* <https://ucsb-cs156.github.io/topics/dokku/deploying_an_app.html>

You will need the environment variables documented in the file `/docs/environment-variables.md` in this repo.

You will also need the command:

* <tt>dokku git:set <i>appname</i> keep-git-dir true</tt>

# Short Version

This short version omits many details, but if you are already familiar with the process of deploying applications, you may be able to use this.

Note that you may need to modify:
* `frontiers` to `frontiers-qa` or `frontiers-cgaucho` (where `cgaucho` is your github id)
* `https://github.com/ucsb-cs156/proj-frontiers` to `https://github.com/ucsb-cs156-f25/proj-frontiers-f24-17` or whatever your repo's url is
* `main` to `my-branch-name` for your feature branch
* `yourEmail@ucsb.edu` to your own email
* values for `CLIENT_ID`, `CLIENT_SECRET` etc from [Google](https://github.com/ucsb-cs156/proj-frontiers/blob/main/docs/oauth.md) or [Github](https://github.com/ucsb-cs156/proj-frontiers/blob/main/docs/github-app-setup-dokku.md) as appropriate

```
dokku apps:create frontiers
dokku git:set appname keep-git-dir true
dokku config:set --no-restart frontiers PRODUCTION=true
dokku config:set --no-restart frontiers SOURCE_REPO=https://github.com/ucsb-cs156/proj-frontiers
dokku postgres:create frontiers-db
dokku postgres:link frontiers-db frontiers
dokku git:sync frontiers https://github.com/ucsb-cs156/proj-frontiers main
dokku ps:rebuild frontiers
dokku letsencrypt:set frontiers email yourEmail@ucsb.edu
dokku letsencrypt:enable frontiers
dokku config:set frontiers --no-restart GOOGLE_CLIENT_ID=get-value-from-google
dokku config:set frontiers --no-restart GOOGLE_CLIENT_SECRET=get-value-from-google
dokku config:set frontiers --no-restart GITHUB_CLIENT_ID=get-value-from-github
dokku config:set frontiers --no-restart GITHUB_CLIENT_SECRET=get-value-from-github
dokku config:set frontiers --no-restart app_private_key="-----BEGIN PRIVATE KEY-----
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
see detailed instructions in github-app-setup-dokku.md
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
xxxxxxxxxxxxxxxxxxxxxxxxx
-----END PRIVATE KEY-----"
dokku ps:rebuild frontiers
```

