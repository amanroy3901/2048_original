# QA Automation Mapping Document

This document provides a comprehensive mapping of the UI elements in the 2048 game for QA automation purposes (Appium, Espresso, etc.).

## Naming Convention
All `testTag`s follow the strict format:
`<ScreenName>_<ComponentType>_<Purpose>_<StateOptional>`

## Screen-wise Mapping

### Splash Screen
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Root Container | `SplashScreen_Root` | The root container of the Splash Screen. | `AccessibilityId("SplashScreen_Root")` |

### Google Auth Screen
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Root Container | `GoogleAuthScreen_Root` | The root container of the Auth Screen. | `AccessibilityId("GoogleAuthScreen_Root")` |
| Google Sign In Button | `GoogleAuthScreen_Button_GoogleSignIn` | Button to initiate Google Sign In. | `AccessibilityId("GoogleAuthScreen_Button_GoogleSignIn")` |
| Play Guest Button | `GoogleAuthScreen_Button_PlayAsGuest` | Button to play as a guest without signing in. | `AccessibilityId("GoogleAuthScreen_Button_PlayAsGuest")` |

### Main Screen
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Root Container | `MainScreen_Root` | The root container of the Main Screen. | `AccessibilityId("MainScreen_Root")` |
| Edit Name Button | `MainScreen_Button_EditName` | Button to open the name edit dialog. | `AccessibilityId("MainScreen_Button_EditName")` |
| Resume Game Button | `MainScreen_Button_ResumeGame` | Button to resume a saved game. | `AccessibilityId("MainScreen_Button_ResumeGame")` |
| Play Game Button | `MainScreen_Button_PlayGame` | Button to start a new game or continue. | `AccessibilityId("MainScreen_Button_PlayGame")` |
| Unity Banner Ad | `MainScreen_Banner_UnityAd` | Banner advertisement displayed on the main screen. | `AccessibilityId("MainScreen_Banner_UnityAd")` |

#### Name Edit Dialog
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Name Input Field | `MainScreen_TextField_NameInput` | Text field to enter player name. | `AccessibilityId("MainScreen_TextField_NameInput")` |
| Cancel Button | `MainScreen_Button_CancelEditName` | Button to cancel name editing. | `AccessibilityId("MainScreen_Button_CancelEditName")` |
| Save Button | `MainScreen_Button_SaveEditName` | Button to save the new name. | `AccessibilityId("MainScreen_Button_SaveEditName")` |

### Game Screen
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Root Container | `GameScreen_Root` | The root container of the Game Screen. | `AccessibilityId("GameScreen_Root")` |
| New Game Button | `GameScreen_Button_NewGame` | Button to open Grid Size dialog for new game. | `AccessibilityId("GameScreen_Button_NewGame")` |
| Undo Button | `GameScreen_Button_Undo` | Button to undo the last move. | `AccessibilityId("GameScreen_Button_Undo")` |
| Close Game Button | `GameScreen_Button_CloseGame` | Button to close the current game. | `AccessibilityId("GameScreen_Button_CloseGame")` |
| Game Cell | `GameScreen_Item_GameCell_{row}_{col}` | Dynamic grid cells. Replace {row} and {col} with indices (e.g., 0_0). | `AccessibilityId("GameScreen_Item_GameCell_0_0")` |

#### Grid Size Dialog
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Dialog Root | `GameScreen_Dialog_GridSize` | Root container of the Grid Size selection dialog. | `AccessibilityId("GameScreen_Dialog_GridSize")` |
| 3x3 Button | `GameScreen_Button_GridSize3x3` | Select 3x3 grid size. | `AccessibilityId("GameScreen_Button_GridSize3x3")` |
| 4x4 Button | `GameScreen_Button_GridSize4x4` | Select 4x4 grid size. | `AccessibilityId("GameScreen_Button_GridSize4x4")` |
| 5x5 Button | `GameScreen_Button_GridSize5x5` | Select 5x5 grid size. | `AccessibilityId("GameScreen_Button_GridSize5x5")` |
| 6x6 Button | `GameScreen_Button_GridSize6x6` | Select 6x6 grid size. | `AccessibilityId("GameScreen_Button_GridSize6x6")` |
| Cancel Button | `GameScreen_Button_CancelGridSize` | Cancel grid selection. | `AccessibilityId("GameScreen_Button_CancelGridSize")` |

#### Exit Dialog
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Cancel Button | `GameScreen_Button_CancelExit` | Cancel exit action. | `AccessibilityId("GameScreen_Button_CancelExit")` |
| Save & Exit Button | `GameScreen_Button_SaveAndExit` | Save game state and exit. | `AccessibilityId("GameScreen_Button_SaveAndExit")` |

#### Game Over Dialog
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Dialog Root | `GameScreen_Dialog_GameOver` | Root container of the Game Over dialog. | `AccessibilityId("GameScreen_Dialog_GameOver")` |
| New Game Button | `GameScreen_Button_GameOverNewGame` | Start a new game from Game Over screen. | `AccessibilityId("GameScreen_Button_GameOverNewGame")` |
| Exit Button | `GameScreen_Button_GameOverExit` | Exit from Game Over screen. | `AccessibilityId("GameScreen_Button_GameOverExit")` |

### Level Unlock Dialog
| Component | testTag | Description | Locator Strategy |
|-----------|---------|-------------|------------------|
| Dialog Root | `LevelUnlockDialog_Root` | Root container of the Level Unlock dialog. | `AccessibilityId("LevelUnlockDialog_Root")` |
| Continue Button | `LevelUnlockDialog_Button_Continue` | Continue playing after unlocking a level. | `AccessibilityId("LevelUnlockDialog_Button_Continue")` |

## Notes for QA
- All interactive elements have `AccessibilityId` matching the `testTag`.
- Stateful buttons (like `NeonRoundedButton`) expose their state ("Enabled" / "Disabled") via `stateDescription`.
- Dynamic lists/grids (like Game Grid) use deterministic IDs based on their position.
