# Github Pages

This repo contains Github Actions workflows (under the `.github/workflows` directory) that
can automatically publish documentation to a `gh-pages` branch, and publish documentation
for the code on the Github Pages site associated with the repo.

This file explains the necessary one-time setup steps to publish this documentation.

After that, the scripts should keep it automatically up-to-date, but if you need to manually regenerate it, the information
below explains how to do that.

When you first pull from this repo into another one, you might get a red X on the Github actions that publish the Github pages site;
following the instructions below should take care of that.

# Steps to setup Github Pages for this repo

## Step 1: Launch  `02-gh-pages-rebuild-part-1` action

Go to the `Actions` menu, and launch workflow `02-gh-pages-rebuild-part-1` as shown. You
 * select the job in the left hand navigation,
 * click on right where it says 'Run workflow'
 * then select `main` branch and click the green `Run Workflow` button **ONCE**.  **DO NOT CLICK IT TWICE**.

Note that it can take a long time for the display to update, and you may even need to refresh the page.  If you click the button twice (because you don't think it "heard you the first time"), it will just take longer and possible fail.
   
<img width="1307" alt="Run Workflow: 02-gh-pages-rebuild-part-1" src="https://github.com/ucsb-cs156-m23/STARTER-jpa03/assets/1119017/fb506c1f-dc95-4e33-9cfa-f91324705c87">

You now need to wait for this job to finish, kick off job `04-gh-pages-rebuild-part-2`, and then complete.

It will create the Github Pages site for the repo, with links to documentation for both the backend (javadoc) and
frontend (storybook).

It is not unusual for this job to fail (have a red X) the first time you run it.  Use the `Rerun Failed Jobs` option to just rerun the parts that failed.

<details markdown="1">
<summary markdown="1">
Click the triangle to read about how to re-run failed Github Actions
</summary>

Here's how to re-run failed Github Actions:

1. Go to the Github Actions tab of your repo
2. On the left side, in the list of workflows, if the one you are looking for is not listed, click `Show More Workflows`, as shown here:

<img width="317" alt="image" src="https://github.com/user-attachments/assets/5a1b6386-460a-484a-a89b-ec8251a81504">

3. On a failed workflow, if you click it, there should be a button like this at the upper right to re-run the failed workflow:

<img width="278" alt="image" src="https://github.com/user-attachments/assets/c9740071-4941-40ba-9948-7ed492f9aaeb">

Alternatively, find the button upper left that says "Run Workflow", click it, and then click the green `Run Workflow` button, as shown here:
   
<img width="920" alt="image" src="https://github.com/user-attachments/assets/ef8c6b89-77fd-4d29-8367-09441ab8e5d1">

</details>

## Step 2: Enable Github Pages

Enable Github Pages on the repo settings as shown below.

Select `Settings`, then `Pages`, then change:
 * `Source` to `Deploy from a branch`
 * `Branch` to `gh-pages` (if you don't see a `gh-pages` branch, you may need to wait for the jobs launched in step 1 to finish).
 * `Select Folder` should show `/ (root)`
  
As shown here:
   
<img width="606" alt="image" src="https://github.com/ucsb-cs156-m23/STARTER-jpa03/assets/1119017/4b762858-0b2d-42ad-a778-94680c50015a">

## Step 3: Set up Link on Main Repo Page

Return to the main page for the repo,  click on the gear at right, and click the box for Github Pages, as shown below
   
   ![add-gh-pages-link](https://user-images.githubusercontent.com/1119017/235330985-1d181d00-c775-4c93-aec1-87414467e0ed.gif)

## Step 4: Check link

Check that the link loads the Github Pages site.  It should look something like this, but with your repo name in place of the one shown.
   
If it doesn't come up right away, check to see whether the  `02-gh-pages-rebuild`  has finished yet.  You may find that it takes a minute or two for the page to become available, and another minute or two before the `javadoc` and `storybook` links for the main branch begin working; but within a minute or two of the  `02-gh-pages-rebuild` finishing both links should work.

<img width="500" alt="image" src="https://user-images.githubusercontent.com/1119017/235750584-2e66dc07-12b3-4593-a289-7e2f2b2060c2.png">
   
# What should it look like?

When it works, the top level page should look something like this:

<img width="400" alt="image" src="https://github.com/user-attachments/assets/60de1d4d-42aa-4ae1-b8e6-fba997bd1571">

The javadoc should look something like this:

<img width="400" alt="image" src="https://user-images.githubusercontent.com/1119017/235764018-3de19026-bab8-4308-93bf-7cad2679cbf6.png">

The storybook should look something like this:

<img width="864" alt="image" src="https://github.com/user-attachments/assets/7f586b76-fbc6-4df8-8fc0-f99a145c413c">

Note that the Storybook may not appear unless/until you update the `CHROMATIC_PROJECT_TOKEN` which is typically a later
stage in the setup instructions documented here: <https://ucsb-cs156.github.io/topics/chromatic/>

# Keeping the site up to date

As you add pull requests, the javadoc and storybook will be generated for those as well by GitHub Actions scripts. 

Note that:
* The javadoc is only generated when there is a change to the backend code (either files under `src/` or the `pom.xml` file)
* The storybook is only generated when there is a change to the frontend code (files under `frontend/`)

# Regenerating the site

If at any point, you want to rebuild the entire documentation site, you can run the GitHub Action `02-gh-pages-rebuild`
again.

   
