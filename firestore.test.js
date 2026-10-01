const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Unauthenticated user: cannot create user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice",
      avatarId: "avatar_crown",
      coins: 1000,
      gems: 20,
      matchesPlayed: 0,
      matchesWon: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Alice King",
      avatarId: "avatar_crown",
      coins: 1000,
      gems: 20,
      matchesPlayed: 0,
      matchesWon: 0,
      unlockedDiceSkins: ["skin_default"],
      unlockedThemes: ["theme_royal"],
      selectedDiceSkin: "skin_default",
      selectedTheme: "theme_royal",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot write another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "Bob",
      avatarId: "avatar_lion",
      coins: 500,
      gems: 10,
      matchesPlayed: 0,
      matchesWon: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create a match record in own subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_1").set({
      matchId: "match_1",
      userId: ALICE_UID,
      gameMode: "Offline",
      won: true,
      coinsEarned: 500,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create match record for another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("matches").doc("match_1").set({
      matchId: "match_1",
      userId: BOB_UID,
      gameMode: "Offline",
      won: true,
      coinsEarned: 500,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create a valid game room", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("rooms").doc("ROOM12").set({
      roomId: "ROOM12",
      hostId: ALICE_UID,
      hostName: "Alice King",
      stake: 500,
      theme: "theme_royal",
      status: "waiting",
      playerCount: 1,
      playerIds: [ALICE_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
