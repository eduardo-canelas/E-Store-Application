# 🛒 Nile Dot Com - E-Commerce Simulation Application

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Swing GUI](https://img.shields.io/badge/GUI-Java%20Swing-blue.svg)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![License](https://img.shields.io/badge/License-Educational-green.svg)]()

A sophisticated **Java Swing desktop application** that simulates a complete e-commerce shopping experience. Built as part of CNT 4714 coursework, this application demonstrates advanced GUI programming, file I/O operations, and business logic implementation.

## 🌟 Key Features

### 💼 **Business Logic**

-   **Smart Inventory Management**: Real-time stock validation with quantity limits
-   **Dynamic Pricing System**: Automatic quantity-based discounts (10%, 15%, 20%)
-   **Transaction Processing**: Complete checkout with tax calculations (6% sales tax)
-   **Shopping Cart**: Support for up to 5 items with add/remove functionality

### 🎨 **User Experience**

-   **Intuitive GUI**: Clean Java Swing interface with responsive design
-   **Real-time Validation**: Instant feedback for stock availability and input errors
-   **Transaction History**: Persistent storage of all completed purchases
-   **Professional Invoicing**: Detailed receipts with timestamps and transaction IDs

### 🔧 **Technical Implementation**

-   **File-based Database**: CSV inventory management system
-   **Event-driven Architecture**: Comprehensive button state management
-   **Error Handling**: Robust validation for all user inputs
-   **Data Persistence**: Automatic transaction logging with timestamps

## 🚀 Quick Start

### Prerequisites

-   Java 8 or higher installed on your system
-   Terminal/Command Prompt access

### Installation & Usage

1. **Clone the repository**

    ```bash
    git clone https://github.com/eduardo-canelas/E-Store-Application.git
    cd E-Store-Application
    ```

2. **Compile the application**

    ```bash
    javac NileDotCom.java
    ```

3. **Run the application**
    ```bash
    java NileDotCom
    ```

### 🎯 **How to Use**

1. **Search Products**: Enter an Item ID and quantity, then click "Search For Item"
2. **Add to Cart**: After finding a product, click "Add Item To Cart"
3. **Manage Cart**: Use "Delete Last Item" to remove items or "Empty Cart" to clear all
4. **Checkout**: Click "Check Out" to complete your purchase and generate an invoice
5. **View History**: Check `transactions.csv` for purchase history

## 📊 Sample Inventory

The application comes with a diverse inventory of 45+ items including:

-   **Electronics**: USB cables, ink cartridges, desk fans
-   **Office Supplies**: Staplers, screwdrivers, index cards
-   **Accessories**: Helmets, sunglasses, calendars
-   **And much more!**

## 🏗️ Architecture & Design Patterns

### **Model-View-Controller (MVC) Inspired Design**

-   **Model**: Inventory and transaction data management
-   **View**: Java Swing GUI components
-   **Controller**: Event listeners and business logic

### **Key Components**

-   `NileDotCom.java`: Main application class with GUI and business logic
-   `inventory.csv`: Product database with pricing and availability
-   `transactions.csv`: Transaction history and audit trail
-   `FileReaderTest.java`: Utility class for file operations

## 💡 Technical Highlights

### **Skills Demonstrated**

-   **Java Swing GUI Development**: Complex layout management and component interaction
-   **File I/O Operations**: CSV reading/writing with proper error handling
-   **Event-Driven Programming**: Comprehensive action listeners and state management
-   **Data Validation**: Input sanitization and business rule enforcement
-   **Object-Oriented Design**: Clean class structure with proper encapsulation

### **Advanced Features**

-   **Dynamic Discount Calculation**: Quantity-based pricing algorithms
-   **State Management**: Smart button enabling/disabling based on application state
-   **Data Persistence**: Automatic transaction logging with unique ID generation
-   **Memory Management**: Efficient ArrayList and HashMap usage

## 📧 Contact

**Eduardo Canelas**

-   GitHub: [eduardo-canelas](https://github.com/eduardo-canelas)

---

*This project showcases practical application of Java programming concepts, GUI development, and software engineering principles in an enterprise simulation environment.*
