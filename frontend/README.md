Tech Stack
Framework: React 19 (Vite 6)

Styling: Tailwind CSS v3

Routing: React Router 7

Icons/UI: Custom Tailwind Components

📂 Project Structure
src/App.jsx: Main routing logic and persistent Layout wrapper.

src/Sidebar.jsx: Unified navigation component with active-state tracking.

src/LoginPage.jsx: Mock authentication portal.

src/Dashboard.jsx: Overview of system health, inventory counts, and expiration alerts.

src/OperationsPortal.jsx: Interface for executing record_donation and record_distribution procedures.

src/CommunityManagement.jsx: CRUD interface for Volunteers, Donors, and Beneficiaries.

🏗 Architecture: Shell & Content Pattern
To maintain a consistent user experience, the application uses a Persistent Layout pattern.

The Shell (App.jsx + Sidebar.jsx): Handles the sidebar, navigation, and overall page structure.

The Content: Individual pages (Dashboard, Operations, etc.) only contain the specific UI for that route. They are "injected" into the layout, preventing unnecessary re-renders of the navigation system.

📊 Database Integration Points
This frontend is designed to interface with the following primaryfeed_db objects:

Views: vw_expiring_inventory (Dashboard) and vw_volunteer_hours_log.

Stored Procedures: record_donation and record_distribution (Operations Portal).

Triggers: Logic to handle volunteer shift overlaps (Community Management).
