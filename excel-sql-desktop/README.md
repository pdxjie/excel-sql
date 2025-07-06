# Excel SQL Desktop

A desktop application for SQL operations on Excel files, built with Electron, Vue 3, and Ant Design Vue.

## Features

### Excel File Management
- Open .xlsx, .xls, and .csv files as queryable datasets
- Create new Excel files with editable data structures
- Track recently used files for quick access
- View file structure with sheets in a tree view

### Sheet Management
- Create new sheets with custom names and field structures
- Rename, duplicate, and delete sheets
- Reorder sheets via drag and drop
- View field metadata (name, type, required status, default values)
- Import data from CSV, JSON, or SQL scripts
- Export sheets to CSV, JSON, or Excel formats

### SQL Editor
- SQL editor with syntax highlighting, auto-completion, and formatting
- Multi-tab editing for parallel script processing
- Support for standard SQL operations (SELECT, INSERT, UPDATE, DELETE)
- SQL execution history tracking
- Save and load SQL query templates

### Query Results
- View query results in a tabular format with pagination, sorting, and filtering
- Edit table cells and generate corresponding SQL update statements
- Advanced filtering and grouping capabilities
- Export query results to Excel, CSV, or JSON

### Data Modeling
- Visual field editing (name, type, comments)
- Configure virtual primary keys and unique constraints
- Create virtual relationships between sheets
- Import/export structure templates for reuse

### Data Cleaning
- Field uniqueness validation
- Null value detection and default value application
- Field type conversion (text to date, number, etc.)
- Find and replace functionality

### User Experience
- Light/dark theme support
- Multi-language interface (English/Chinese)
- Customizable workspace layout

## Getting Started

### Prerequisites
- Node.js (v14+)
- npm or yarn

### Installation

1. Clone the repository:
```
git clone https://github.com/yourusername/excel-sql-desktop.git
cd excel-sql-desktop
```

2. Install dependencies:
```
npm install
```

3. Run the application in development mode:
```
npm run dev
```

4. Build the application:
```
npm run build
```

## Technology Stack

- **Electron**: Cross-platform desktop application framework
- **Vue 3**: Frontend framework with Composition API
- **TypeScript**: Type-safe JavaScript
- **Ant Design Vue**: UI component library
- **Monaco Editor**: Code editor for SQL
- **AlaSQL**: In-memory SQL database
- **XLSX**: Excel file parsing and generation
- **SQL Formatter**: SQL query formatting

## License

This project is licensed under the ISC License. 