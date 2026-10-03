// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import {
  getAuth,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  onAuthStateChanged
} from "firebase/auth";
import {
  getFirestore,
  doc,
  setDoc,
  getDoc,
  updateDoc
} from "firebase/firestore";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyC3WdHqJOnVtgBh5KMS5aWulSqpOplQatk",
  authDomain: "myludoapp-97629.firebaseapp.com",
  projectId: "myludoapp-97629",
  storageBucket: "myludoapp-97629.firebasestorage.app",
  messagingSenderId: "1008445954517",
  appId: "1:1008445954517:web:48ed38d9bfef8a3bbc055c",
  measurementId: "G-LY237D4V7D"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = typeof window !== "undefined" ? getAnalytics(app) : null;

export const auth = getAuth(app);
export const db = getFirestore(app);

// 1. User Sign Up Handler
export const handleSignUp = async (email, password, name) => {
  try {
    // 1. Firebase Auth me user banayein
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    const user = userCredential.user;

    // 2. Firestore me user ka profile + initial state save karein
    await setDoc(doc(db, "users", user.uid), {
      uid: user.uid,
      name: name,
      email: email,
      lastScreen: "HomeScreen", // Jahan se game ya profile shuru hoga
      gameState: null,
      updatedAt: new Date()
    });

    console.log("Signup successful!");
    return user;
  } catch (error) {
    console.error("Signup Error:", error.message);
    throw error;
  }
};

// 2. User Login Handler
export const handleLogin = async (email, password, navigation) => {
  try {
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    const user = userCredential.user;

    // User ka last saved state Firestore se fetch karein
    const userDoc = await getDoc(doc(db, "users", user.uid));
    
    if (userDoc.exists()) {
      const userData = userDoc.data();
      const lastScreen = userData.lastScreen || "HomeScreen";
      
      // User ko usi screen par redirect karein jahan usne chhoda tha
      if (navigation && typeof navigation.navigate === "function") {
        navigation.navigate(lastScreen, { savedState: userData.gameState });
      }
      return userData;
    }
    return null;
  } catch (error) {
    console.error("Login Error:", error.message);
    throw error;
  }
};

// 3. Save User State / Resume Where Left
export const saveUserState = async (screenName, currentProgress) => {
  if (auth.currentUser) {
    const userRef = doc(db, "users", auth.currentUser.uid);
    await updateDoc(userRef, {
      lastScreen: screenName,
      gameState: currentProgress,
      updatedAt: new Date()
    });
  }
};

// 4. Auth State Changed Listener
export const initAuthListener = (onUserLoggedIn, onUserLoggedOut) => {
  return onAuthStateChanged(auth, async (user) => {
    if (user) {
      // User already logged in hai, uska last state uthakar direct redirect karein
      const userDoc = await getDoc(doc(db, "users", user.uid));
      if (userDoc.exists()) {
        const userData = userDoc.data();
        if (onUserLoggedIn) {
          onUserLoggedIn(user, userData);
        }
      } else {
        if (onUserLoggedIn) {
          onUserLoggedIn(user, null);
        }
      }
    } else {
      // User logged in nahi hai, SignUp/Login screen dikhayein
      if (onUserLoggedOut) {
        onUserLoggedOut();
      }
    }
  });
};

export { app, analytics, firebaseConfig };
export default firebaseConfig;
