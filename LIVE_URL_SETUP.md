# How To Create The Live URL

Use GitHub Pages to create a public project demo page for this Android APK project.

## Step 1: Push Project To GitHub

Create a public GitHub repository, then push this project.

```powershell
git add .gitignore README.md SUBMISSION_STATUS.md LIVE_URL_SETUP.md index.html app build.gradle.kts settings.gradle.kts gradle gradlew gradlew.bat gradle.properties emulator_screen.png emulator_screen_after.png emulator_screen_pull.png emulator_screen_sw.png
git commit -m "add Android project and demo page"
git branch -M master
git remote add origin https://github.com/YOUR-USERNAME/Mahila-Shakti-Unnati.git
git push -u origin master
```

Replace `YOUR-USERNAME` with your GitHub username.

## Step 2: Enable GitHub Pages

1. Open your GitHub repository.
2. Go to **Settings**.
3. Click **Pages** from the left side menu.
4. Under **Build and deployment**, select **Deploy from a branch**.
5. Under **Branch**, select:
   - Branch: `master`
   - Folder: `/root`
6. Click **Save**.

## Step 3: Copy The Live URL

After a few minutes, GitHub will show a live URL like:

```text
https://YOUR-USERNAME.github.io/Mahila-Shakti-Unnati/
```

Use that URL in the evaluation form under **Live URL**.

For this project, the correct URL format is:

```text
https://sirib219.github.io/Mahila-Shakti_Unnati/
```

## Important

The Android app itself does not run in the browser. This live URL is a public demo page for the APK project, showing the project description, features, screenshots, APK download, GitHub link, and technologies used.

The GitHub Repository button inside `index.html` has already been updated to:

```text
https://github.com/Sirib219/Mahila-Shakti_Unnati
```
