# Firebase Google Sign-In Setup Checklist

## 🔴 CRITICAL ISSUE FOUND
Your `google-services.json` shows an empty `oauth_client` array, which means Google Sign-In is not configured in Firebase.

## Required Steps to Fix Google Sign-In

### 1. Add SHA-1 Fingerprint to Firebase
```bash
# Get your SHA-1 fingerprint
cd android
./gradlew signingReport
```
Look for the SHA1 under `Variant: debug, Config: debug`

### 2. Enable Google Sign-In in Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project "original-64f6c"
3. Go to **Authentication** → **Sign-in method**
4. Click **Add new provider** → **Google**
5. Enable it and add your **Web Client ID**

### 3. Get Web Client ID
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project
3. Go to **APIs & Services** → **Credentials**
4. Create OAuth 2.0 Client ID (Web application)
5. Copy the **Client ID** (looks like: `305513134474-v65v4qb636fnkhvrbl7k9m9tpu07ap5v.apps.googleusercontent.com`)

### 4. Update Your Code
The current Web Client ID in your code:
```kotlin
.setServerClientId("305513134474-v65v4qb636fnkhvrbl7k9m9tpu07ap5v.apps.googleusercontent.com")
```

### 5. Download Updated google-services.json
After configuring Google Sign-In in Firebase:
1. Go to Firebase Console → Project Settings
2. Download the updated `google-services.json`
3. Replace the file in `app/google-services.json`

### 6. Common Issues to Check
- [ ] SHA-1 fingerprint added to Firebase
- [ ] Google Sign-In enabled in Firebase Console
- [ ] Web Client ID is correct
- [ ] Package name matches: `com.avfusionapps.game_2048`
- [ ] google-services.json file is in the correct location

### 7. Test the Fix
After completing these steps:
1. Clean and rebuild the project
2. Install the app
3. Test Google Sign-In

## Current Error Explanation
The "Sign-in cancelled or failed" error occurs because:
- Firebase doesn't recognize your app as authorized
- No OAuth clients are configured
- The Credential Manager can't get valid credentials

## Need Help?
If you're still having issues after following these steps, please share:
1. Your SHA-1 fingerprint
2. Screenshot of Firebase Console → Authentication → Sign-in method
3. Screenshot of Google Cloud Console → Credentials