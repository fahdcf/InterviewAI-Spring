# UI source inventory

Recorded on 21 November 2026 from the pinned legacy snapshot and migrated FXML. The tables list inputs, charts, and controls with actions. The runtime checks and known placeholders are recorded in [Legacy acceptance](legacy-acceptance.md).

## Original application

### AdminProfileView.fxml

Controller com.interviewai.controller.AdminProfileController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextField | nameField | Enter your display name |  |
| Button | editNameBtn | ✏ | onAction=#onEditName |
| Button | saveNameBtn | ✓ | onAction=#onSaveName |
| Button | cancelNameBtn | ✕ | onAction=#onCancelEditName |
| TextField | emailField | Email address |  |
| TextField | usernameField | Username |  |
| PasswordField | currentPasswordField | Enter current password |  |
| PasswordField | newPasswordField | Enter new password |  |
| PasswordField | confirmPasswordField | Confirm new password |  |
| Button | changePasswordBtn | Change Password | onAction=#onChangePassword |
| Button |  | 🚪 End Session & Logout | onAction=#onLogout |

### AdminView.fxml

Controller com.interviewai.controller.AdminController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| LineChart | signupTrendChart |  |  |
| LineChart | usageTrendChart |  |  |
| PieChart | interviewTypePieChart |  |  |
| PieChart | languagePieChart |  |  |

### CourseMapView.fxml

Controller com.interviewai.controller.CourseMapController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |

### CoursesManagementView.fxml

Controller com.interviewai.controller.CoursesManagementController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextField | searchField | Search by course title, owner name or email... |  |
| ComboBox | statusFilter | All Statuses |  |
| Button |  | Search | onAction=#onSearch |
| Button |  | ✕ Reset | onAction=#onResetFilters |
| TableView | coursesTable |  |  |

### CreateAccountView.fxml

Controller com.interviewai.controller.CreateAccountController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button |  | Login | onAction=#onBack |
| TextField | usernameField | Username |  |
| TextField | emailField | Email |  |
| PasswordField | passwordField | Password |  |
| Button | createAccountButton | SIGN UP | onAction=#onCreate |

### DashboardContent.fxml

Controller com.interviewai.controller.DashboardController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| MenuItem |  | 🎯 Technical | onAction=#onSelectTechnical |
| MenuItem |  | 💬 Soft Skills | onAction=#onSelectSoftSkills |
| MenuItem |  | ➕ New Course | onAction=#onCreateNewCourse |
| MenuItem |  | ⇄ Switch Course | onAction=#onSwitchCourse |
| Button | startChapterButton |  | onAction=#onStartChapter |

### DashboardView.fxml

Controller com.interviewai.controller.DashboardController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button |  |  | onAction=#onOpenHome |
| Button |  |  | onAction=#onOpenProgress |
| Button |  |  | onAction=#onOpenQuests |
| Button |  |  | onAction=#onOpenProfile |
| Button |  |  | onAction=#onLogout |
| MenuItem |  | 🎯 Technical | onAction=#onSelectTechnical |
| MenuItem |  | 💬 Soft Skills | onAction=#onSelectSoftSkills |
| MenuItem |  | ➕ New Course | onAction=#onCreateNewCourse |
| MenuItem |  | ⇄ Switch Course | onAction=#onSwitchCourse |
| Button | startChapterButton |  | onAction=#onStartChapter |

### EditGoalsDialog.fxml

Controller com.interviewai.controller.EditGoalsDialogController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextField | goalNameField | e.g., Master Java Basics |  |
| ComboBox | goalTypeCombo | Select type |  |
| TextField | targetValueField | 100 |  |

### FeedbackView.fxml

Controller com.interviewai.controller.FeedbackController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextArea |  | Feedback details |  |

### LessonView.fxml

Controller com.interviewai.controller.LessonController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextArea | shortAnswerField | Type your answer here... |  |
| Button | showExplanationButton | 📚 Show Explanation | onAction=#onShowExplanation |
| Button | showFeedbackButton | ← Back to Feedback | onAction=#onShowFeedback |
| Button | submitButton | Submit Answer | onAction=#onSubmitAnswer |
| Button | nextButton | Next → | onAction=#onNext |
| Button | finishButton | ✓ Finish Chapter | onAction=#onFinish |

### LoginView.fxml

Controller com.interviewai.controller.LoginController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button |  | Sign Up | onAction=#onOpenCreateAccount |
| TextField | usernameField | Email or username |  |
| PasswordField | passwordField | Password |  |
| Button | loginButton | LOGIN | onAction=#onLogin |
| Button |  | Forgot? | onAction=#onForgotPassword |

### MainLayout.fxml

Controller com.interviewai.controller.MainLayoutController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |

### OnboardingView.fxml

Controller com.interviewai.controller.OnboardingController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| HBox | optionJobInterview |  | onMouseClicked=#onSelectInterviewType |
| HBox | optionVisaInterview |  | onMouseClicked=#onSelectInterviewType |
| HBox | optionInternshipInterview |  | onMouseClicked=#onSelectInterviewType |
| HBox | optionUniversityInterview |  | onMouseClicked=#onSelectInterviewType |
| HBox | optionEnglish |  | onMouseClicked=#onSelectLanguage |
| HBox | optionFrench |  | onMouseClicked=#onSelectLanguage |
| HBox | optionArabic |  | onMouseClicked=#onSelectLanguage |
| HBox | optionSpanish |  | onMouseClicked=#onSelectLanguage |
| HBox | optionTomorrow |  | onMouseClicked=#onSelectTimeline |
| HBox | optionThisWeek |  | onMouseClicked=#onSelectTimeline |
| HBox | optionLater |  | onMouseClicked=#onSelectTimeline |
| TextField | contextField | e.g., Software Developer, Marketing Manager |  |
| TextField | descriptionField | Add more description for better customization |  |
| VBox | uploadArea |  | onMouseClicked=#onUploadCV |
| Button | backButton | Back | onAction=#onBack |
| Button | nextButton | Next | onAction=#onNext |

### OnboardingWaitingView.fxml

Controller com.interviewai.controller.OnboardingWaitingController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |

### ProfileView.fxml

Controller com.interviewai.controller.ProfileController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextField | nameField | Enter your display name |  |
| Button | editNameButton | ✏ | onAction=#onEditName |
| Button | saveNameButton | ✓ | onAction=#onSaveName |
| Button | cancelNameButton | ✕ | onAction=#onCancelName |
| PasswordField | currentPasswordField | Enter current password |  |
| PasswordField | newPasswordField | Enter new password |  |
| PasswordField | confirmPasswordField | Confirm new password |  |
| Button | savePasswordButton | Change Password | onAction=#onSavePassword |
| Button | deleteAccountButton | 🗑 Delete Account | onAction=#onDeleteAccount |

### ProgressView.fxml

Controller com.interviewai.controller.ProgressController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| LineChart | xpChart |  |  |

### PromptView.fxml

Controller com.interviewai.controller.PromptController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextArea |  |  |  |

### QuestsView.fxml

Controller com.interviewai.controller.QuestsController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button | dailyClaimButton | Claim Reward | onAction=#handleClaimDaily |
| Button | monthlyClaimButton | Claim Reward | onAction=#handleClaimMonthly |

### ReviewQuestionsView.fxml

Controller com.interviewai.controller.reviewQuestionsController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextArea | shortAnswerField | Type your answer here... |  |
| Button | showExplanationButton | 📚 Show Explanation | onAction=#onShowExplanation |
| Button | showFeedbackButton | ← Back to Feedback | onAction=#onShowFeedback |
| Button | submitButton | Submit Answer | onAction=#onSubmitAnswer |
| Button | nextButton | Next → | onAction=#onNext |
| Button | finishButton | ✓ Finish Review | onAction=#onFinish |

### ReviewView.fxml

Controller com.interviewai.controller.ReviewController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |

### SidebarComponent.fxml

Controller com.interviewai.controller.SidebarController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button | dashboardButton |  | onAction=#onOpenDashboard |
| Button | lessonButton |  | onAction=#onOpenLesson |
| Button | progressButton |  | onAction=#onOpenProgress |
| Button | reviewButton |  | onAction=#onOpenReview |
| Button | questsButton |  | onAction=#onOpenQuests |
| Button | profileButton |  | onAction=#onOpenProfile |
| Button | logoutButton |  | onAction=#onLogout |

### SimulationView.fxml

Controller com.interviewai.controller.SimulationController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |

### UserManagementView.fxml

Controller com.interviewai.controller.UserManagementController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextField | searchField | Search by username or email... |  |
| ComboBox | filterInterviewType | Interview Type |  |
| ComboBox | filterLanguage | Language |  |
| ComboBox | filterStatus | Status |  |
| ComboBox | filterProgress | Progress |  |
| Button | clearFiltersBtn | ✕ Clear | onAction=#onClearFilters |
| Button | prevPageBtn | ◀ | onAction=#onPrevPage |
| Button | nextPageBtn | ▶ | onAction=#onNextPage |
| TableView | userTable |  |  |
| Button | editProfileBtn |  | onAction=#onEditProfile |
| Button | suspendBtn |  | onAction=#onSuspendUser |
| Button | activateBtn |  | onAction=#onActivateUser |
| Button | resetXPBtn |  | onAction=#onResetXP |
| Button | resetProgressBtn |  | onAction=#onResetProgress |

## Migrated desktop

### admin-view.fxml

Controller com.interviewai.desktop.feature.admin.AdminController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button |  | Refresh | onAction=#refresh |
| Button |  | Profile | onAction=#profile |
| Button |  | Sign out | onAction=#logout |
| Tab | overviewTab | Overview |  |
| LineChart | trends | Recent activity |  |
| PieChart | interviewTypes | Interview types |  |
| PieChart | languages | Languages |  |
| Tab | userTab | Users |  |
| TextField | userSearch | Username or email | onAction=#searchUsers |
| ComboBox | statusFilter | Status |  |
| ComboBox | roleFilter | Role |  |
| Button |  | Search | onAction=#searchUsers |
| ListView | users |  |  |
| Button | userPrevious | Previous | onAction=#previousUsers |
| Button | userNext | Next | onAction=#nextUsers |
| TextField | userEmail | User email |  |
| ComboBox | userRole | Role |  |
| Button |  | Save | onAction=#saveUser |
| Button |  | Suspend / activate | onAction=#toggleUser |
| Button |  | Reset XP | onAction=#resetXp |
| Button |  | Reset progress | onAction=#resetProgress |
| Tab | courseTab | Courses |  |
| TextField | courseSearch | Course title or owner | onAction=#searchCourses |
| ComboBox | courseFilter | Status |  |
| Button |  | Search | onAction=#searchCourses |
| ListView | courses |  |  |
| Button | coursePrevious | Previous | onAction=#previousCourses |
| Button | courseNext | Next | onAction=#nextCourses |
| Button |  | Archive / restore | onAction=#archiveCourse |
| Button |  | Delete course | onAction=#deleteCourse |

### connection-view.fxml

Controller com.interviewai.desktop.feature.connection.ConnectionController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button | retryButton | Check connection | onAction=#checkConnection |
| Button |  | Back to sign-in | onAction=#onBack |

### course-waiting-view.fxml

Controller com.interviewai.desktop.feature.onboarding.CourseWaitingController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button | retryButton | Retry | onAction=#retry |
| Button |  | Back to dashboard | onAction=#back |

### dashboard-view.fxml

Controller com.interviewai.desktop.feature.dashboard.DashboardController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| ComboBox | coursePicker | Select a course | onAction=#selectCourse |
| ListView | chapterList |  |  |
| Button | saveButton | Open lesson | onAction=#openLesson |
| Button | reviewChapterButton | Review this chapter | onAction=#reviewChapter |
| Button | reviewCourseButton | Review this course | onAction=#reviewCourse |
| Button |  | Refresh | onAction=#refresh |
| Button |  | Interview preferences | onAction=#openOnboarding |
| Button |  | Generation status | onAction=#openCourseJob |
| Button |  | Progress and quests | onAction=#openProgress |
| Button |  | Profile | onAction=#openProfile |
| Button | logoutButton | Sign out | onAction=#logout |

### login-view.fxml

Controller com.interviewai.desktop.feature.auth.LoginController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextField | usernameField | Email or username |  |
| PasswordField | passwordField | Password |  |
| Button | loginButton | Sign in | onAction=#onLogin |
| Button | registerButton | Create account | onAction=#onOpenCreateAccount |
| Button |  | Forgot password? | onAction=#onForgotPassword |
| Button |  | Check backend connection | onAction=#onCheckConnection |

### onboarding-view.fxml

Controller com.interviewai.desktop.feature.onboarding.OnboardingController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| ComboBox | interviewType |  |  |
| ComboBox | language |  |  |
| ComboBox | timeline |  |  |
| TextField | context | Role, company, or programme |  |
| TextArea | description |  |  |
| Button |  | Choose CV | onAction=#chooseCv |
| CheckBox | removeCv | Remove saved CV |  |
| Button |  | Save preferences | onAction=#save |
| Button |  | Generate courses | onAction=#generate |
| Button |  | Back to dashboard | onAction=#back |

### practice-view.fxml

Controller com.interviewai.desktop.feature.practice.PracticeController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextArea | answer | Your answer |  |
| Button | submitButton | Submit answer | onAction=#submit |
| Button | skipButton | Skip | onAction=#skip |
| Button | nextButton | Next | onAction=#next |
| Button | explainButton | Explain answer | onAction=#explain |
| Button |  | Back to dashboard | onAction=#back |

### profile-view.fxml

Controller com.interviewai.desktop.feature.profile.ProfileController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextField | username |  |  |
| Button |  | Save username | onAction=#rename |
| PasswordField | currentPassword | Current password |  |
| PasswordField | newPassword | New password |  |
| PasswordField | confirmPassword | Confirm new password |  |
| Button |  | Change password | onAction=#changePassword |
| Button | deleteButton | Delete account | onAction=#delete |
| Button |  | Back | onAction=#back |

### progress-view.fxml

Controller com.interviewai.desktop.feature.progress.ProgressController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| Button |  | Back to dashboard | onAction=#back |
| Button |  | Refresh | onAction=#refresh |
| BarChart | xpChart | XP earned in the last 7 days |  |
| ListView | goals |  |  |
| TextField | goalName | Goal name |  |
| ComboBox | goalType |  |  |
| TextField | goalTarget | Target |  |
| Button |  | Add goal | onAction=#addGoal |
| Button |  | Update selected | onAction=#updateGoal |
| Button |  | Remove selected | onAction=#removeGoal |
| Button | dailyClaim | Claim daily reward | onAction=#claimDaily |
| Button | monthlyClaim | Claim monthly reward | onAction=#claimMonthly |
| ListView | rankings |  |  |

### register-view.fxml

Controller com.interviewai.desktop.feature.auth.RegisterController.

| Control | ID | Label or prompt | Handler |
| --- | --- | --- | --- |
| TextField | usernameField | Username |  |
| TextField | emailField | Email (optional) |  |
| PasswordField | passwordField | Password |  |
| Button | createButton | Create account | onAction=#onCreate |
| Button |  | Back to sign-in | onAction=#onBack |

