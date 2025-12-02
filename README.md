Attendify

Attendify is a simple attendance tracking app built using Kotlin and Jetpack Compose.
We kept in mind the whole time to make it very easy to use for students, so we are the perfect people to make this.

What it does:
	-	Lets you add courses with a code, name, and required attendance %
	-	Shows a dashboard with all courses and their current attendance
	-	Lets you manually check in for today
	-	Lets you add a missed class (yesterday) or a future one (tomorrow)
	-	Shows a detailed page for each course with all sessions listed
	-	Has a QR scan screen for check-ins
	-	Sends a notification if you fall below the required attendance

We built it with:
	-	Kotlin
	-	Jetpack Compose
	-	Room (local database)
	-	ViewModel/Repository setup
	-	NotificationManager for alerts

Notes
	- Everything runs locally with Room, no backend needed
	-	UI is kept simple but functional
  - We didn't make a log-in page due to time constraint
