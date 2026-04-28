# PennyWize – Personal Finance Tracker App

PennyWize is an Android budgeting and expense tracking application designed to help users manage their monthly finances, set savings goals, track expenses, and monitor their remaining budget in real time.

## Features

### Authentication
- User registration and login system
- Session-based user tracking

### Expense Management
- Add, edit, and delete expenses
- Attach descriptions, categories, dates, and optional images
- Filter expenses by date range
- View total spending for selected periods

### Budget & Goals System
- Set monthly minimum and maximum budget goals
- Track remaining balance automatically
- Prevent duplicate goals per month (planned enhancement)

### Dashboard Overview
- Displays:
  - Total monthly expenses
  - Remaining balance
  - Budget limits (min/max goals)
- Real-time financial overview per user

### Financial Health Rating System
A 4-level rating system evaluates the user’s remaining balance:

- **Excellent Control** – Spending is well within budget
- **Healthy Range** – Finances are under control
- **Warning Zone** – Approaching budget limits
- **Critical Overspend** – Budget exceeded or highly unsafe spending

Each rating is color-coded and displayed alongside the remaining balance for quick insight.

### Category Tracking
- Organize expenses by category
- View category-based totals

### Navigation System
- Bottom navigation for quick access:
  - Home
  - Expenses
  - Categories
  - Goals

##Tech Stack

- **Language:** Kotlin  
- **Database:** RoomDb 
- **Architecture:** MVVM (light implementation)  
- **UI:** XML Layouts  
- **Concurrency:** Kotlin Coroutines  
- **Image Loading:** Glide  

##Database Structure

- **User Table**
- **Expense Table**
- **Goal Table**
- **Category Table**

Room DAOs handle all CRUD operations including filtering, updates, and aggregations.

##Core Logic

- Expenses are filtered by date range using SQL queries
- Total expenses are calculated using `sumOf { it.amount }`
- Remaining balance is calculated as:
-   Remaining Balance = Maximum Budget Goal - Total Expenses

- Rating system dynamically evaluates financial health based on remaining balance thresholds

## Future Improvements

- Goal duplication prevention enforcement
- Advanced analytics dashboard (charts & graphs)
- Monthly/weekly spending trends
- Export reports (PDF/CSV)
- Cloud sync support
- Authentication security upgrades

## Author

Developed as a personal finance management project using Android (Kotlin) and Room Database.

##  Note

This project focuses on practical budgeting logic, clean UI structure, and real-world financial tracking behaviour suitable for personal or academic use.
