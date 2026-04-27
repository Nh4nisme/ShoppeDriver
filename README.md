# Shoppe Driver - Distributed Logistics Dispatch System

## Overview

A fully functional **Java-based distributed logistics dispatch system** using JavaFX, multi-threading, blocking queues, and real-time map integration. The system simulates order processing, batch creation, shipper assignment, and delivery tracking with an admin dashboard and individual shipper apps.

## Architecture

### Core Components

#### 1. **Data Models** (`model/`)
- `Order`: Represents an order with ID, coordinates (x, y), and status
- `Batch`: Groups multiple orders with status and shipper assignment
- `Shipper`: Represents a delivery driver with location and status
- Status Enums: `OrderStatus`, `BatchStatus`, `ShipperStatus`

#### 2. **Services** (`service/`)
- **OrderService**: Generates mock orders and pushes them to the order queue
- **RouteBuilderService**: Groups orders into batches using nearest-neighbor sorting
- **DispatcherService**: Assigns batches to the nearest available shippers
- **ShipperTrackingService**: Tracks shipper locations and batch progress

#### 3. **Workers** (`worker/`)
- **ShipperWorker**: Implements `Runnable` for each shipper
  - Manages assigned orders queue
  - Supports auto and manual delivery modes
  - Notifies listeners of state changes

#### 4. **UI Components** (`ui/`)

##### Admin Dashboard (`ui/admin/`)
- **DashboardView**: Main dashboard layout
- **KPIBar**: Shows total orders, in-progress, completed, and active shippers
- **Sidebar**: Lists all batches with status and progress
- **ShipperStatusPanel**: Shows all active shippers and their current location
- **LogPanel**: Event logging console
- **GoogleMapsPanel**: WebView-based map integration

##### Shipper App (`ui/shipper/`)
- **ShipperAppWindow**: Individual window for each shipper
- **ShipperAppView**: Layout for shipper-specific UI
- **OrderListPanel**: Shows pending and current orders
- **OrderDetailPanel**: Displays current order details and distance
- **ControlsPanel**: Buttons for start/stop auto-delivery and manual delivery

#### 5. **Utilities** (`util/`)
- **QueueManager**: Singleton managing blocking queues
- **ThreadPoolManager**: Manages executor service for threads
- **LocationUtil**: Distance and movement calculations
- **DataChangeListener**: Observer pattern interface

### Threading Model

```
Main Thread (JavaFX)
├── OrderService (generates orders)
├── RouteBuilderService (creates batches)
├── DispatcherService (assigns to shippers)
└── 4x ShipperWorker threads (individual delivery)

All services communicate via BlockingQueues:
Orders → orderQueue → RouteBuilderService → batchQueue → DispatcherService → ShipperWorker
```

### Data Flow

1. **OrderService** generates 15 mock orders initially, then 2 every 5 seconds
2. **RouteBuilderService** polls orders, groups 3-5 into batches, sorts via nearest-neighbor
3. **DispatcherService** finds nearest available shipper for each batch
4. **ShipperWorker** receives batch, manages delivery (auto or manual mode)
5. **ShipperTrackingService** tracks all updates and notifies UI listeners
6. **UI Components** update via `Platform.runLater()` for thread safety

## Project Structure

```
ShoppeDriver/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/logistics/
│   │   │       ├── MainApp.java (entry point)
│   │   │       ├── Main.java (delegates to MainApp)
│   │   │       ├── model/
│   │   │       │   ├── Order.java
│   │   │       │   ├── Batch.java
│   │   │       │   ├── Shipper.java
│   │   │       │   ├── OrderStatus.java
│   │   │       │   ├── BatchStatus.java
│   │   │       │   └── ShipperStatus.java
│   │   │       ├── service/
│   │   │       │   ├── OrderService.java
│   │   │       │   ├── RouteBuilderService.java
│   │   │       │   ├── DispatcherService.java
│   │   │       │   └── ShipperTrackingService.java
│   │   │       ├── worker/
│   │   │       │   └── ShipperWorker.java
│   │   │       ├── ui/
│   │   │       │   ├── GoogleMapsPanel.java
│   │   │       │   ├── admin/
│   │   │       │   │   ├── DashboardView.java
│   │   │       │   │   ├── KPIBar.java
│   │   │       │   │   ├── Sidebar.java
│   │   │       │   │   ├── ShipperStatusPanel.java
│   │   │       │   │   └── LogPanel.java
│   │   │       │   └── shipper/
│   │   │       │       ├── ShipperAppWindow.java
│   │   │       │       ├── ShipperAppView.java
│   │   │       │       ├── OrderListPanel.java
│   │   │       │       ├── OrderDetailPanel.java
│   │   │       │       └── ControlsPanel.java
│   │   │       └── util/
│   │   │           ├── QueueManager.java
│   │   │           ├── ThreadPoolManager.java
│   │   │           ├── LocationUtil.java
│   │   │           └── DataChangeListener.java
│   │   └── resources/
│   │       └── map/
│   │           └── map.html (Google Maps integration)
│   └── test/
│       └── java/
```

## Setup & Dependencies

### Prerequisites
- **Java 21+**
- **Maven 3.6+**

### Dependencies
- **JavaFX 21.0.2** - UI framework
- **Google GSON 2.10.1** - JSON serialization
- **Maven 3.11.0** - Build tool
- **JavaFX Maven Plugin 0.0.8** - Maven integration

### Installation

1. **Clone or copy the project:**
   ```bash
   cd ShoppeDriver
   ```

2. **Download dependencies:**
   ```bash
   mvn clean install
   ```

3. **Compile:**
   ```bash
   mvn compile
   ```

## Running the Application

### Option 1: Using Maven (Recommended)

```bash
mvn javafx:run
```

### Option 2: Using IDE (IntelliJ IDEA)

1. Open the project in IntelliJ IDEA
2. Right-click `MainApp.java` → Run 'MainApp.main()'

## How It Works

### 1. Application Start

When you run the application:
1. JavaFX starts and loads the **Admin Dashboard**
2. Four shipper windows open automatically (Alice, Bob, Charlie, Diana)
3. Background services start running
4. Order generation begins

### 2. Order Processing Flow

```
OrderService generates orders randomly
    ↓
Orders sit in orderQueue
    ↓
RouteBuilderService batches them (3-5 orders per batch)
    ↓
Orders marked IN_DELIVERY, sorted by nearest-neighbor
    ↓
Batches sit in batchQueue
    ↓
DispatcherService finds nearest available shipper
    ↓
Batch assigned to shipper, shipper status → IN_DELIVERY
    ↓
ShipperWorker receives batch, begins delivery
```

### 3. Using the Admin Dashboard

**Left Panel (Sidebar):**
- Lists all batches and their status
- Shows progress (delivered/total orders)
- Shipper assignment info

**Top Panel (KPI Bar):**
- Total orders in system
- Orders in progress
- Completed deliveries
- Number of active shippers

**Center Panel (Map):**
- Google Maps showing shipper locations (blue markers)
- Order locations (red=pending, yellow=in-delivery, green=delivered)
- Real-time updates every 1 second

**Right Panel (Shipper Status):**
- All active shippers
- Current status
- Current location coordinates

**Bottom Panel (Log):**
- System events with timestamps
- Order assignments, deliveries, service status

### 4. Using Shipper Apps

Each shipper window shows:

**Top Section:**
- Shipper name, ID, and status

**Left Section (Order List):**
- All assigned orders
- Current delivery highlighted in yellow

**Right Section (Order Detail):**
- Current order ID and destination
- Your current location
- Distance to order

**Bottom Section (Controls):**
- **Start Auto-Delivery**: Automatic movement and delivery every second
- **Stop Auto-Delivery**: Pause automatic mode
- **Deliver Next (Manual)**: Immediately deliver next order
- Progress bar showing pending orders

## Simulation Features

### Mock Data
- **15 initial orders** with random coordinates (0-100 range)
- **2 new orders every 5 seconds**
- **4 shippers** starting at positions (10,10), (20,20), (30,30), (40,40)

### Delivery Modes

**Auto Mode:**
- Shipper automatically moves towards orders
- Movement speed: 0.1 units per update
- Delivery automatically triggers when close enough
- Continues until all orders delivered

**Manual Mode:**
- Click "Deliver Next" to instantly deliver next order
- Useful for testing without waiting

### Nearest-Neighbor Routing
Orders in a batch are sorted to minimize travel distance:
1. Start from first order
2. Always move to nearest unvisited order
3. Repeat until all visited

## Key Design Patterns

### 1. Observer Pattern
- UI components implement `DataChangeListener`
- Services notify listeners on state changes
- Updates via `Platform.runLater()` for thread safety

### 2. Thread-Safe Data Structures
- `AtomicReference<T>` for thread-safe fields
- `ConcurrentHashMap` for shared data
- `CopyOnWriteArrayList` for listener lists
- `BlockingQueue` for service communication

### 3. Singleton Pattern
- `QueueManager`, `ThreadPoolManager`, `ShipperTrackingService`
- Single instance across application
- Thread-safe lazy initialization

### 4. Producer-Consumer Pattern
```
Producer: OrderService → orderQueue
Consumer: RouteBuilderService
Producer: RouteBuilderService → batchQueue
Consumer: DispatcherService → ShipperWorker
```

## Configuration

### Adjustable Parameters

Edit `OrderService.java`:
```java
generateMockOrders(15);  // Initial order count
Thread.sleep(5000);      // Order generation interval
generateMockOrders(2);   // New orders per interval
```

Edit `RouteBuilderService.java`:
```java
private final int BATCH_SIZE_MIN = 3;  // Minimum orders per batch
private final int BATCH_SIZE_MAX = 5;  // Maximum orders per batch
```

Edit `ShipperWorker.java`:
```java
Thread.sleep(1000);  // Auto-delivery update interval
MOVE_SPEED = 0.1;    // Units per update in LocationUtil
```

## Troubleshooting

### Issue: "Module not found" or "Cannot find symbol"

**Solution:** Ensure all dependencies are downloaded:
```bash
mvn clean dependency:resolve
mvn compile
```

### Issue: Map not showing in UI

**Solution:** 
1. Google Maps API requires internet connection
2. Replace `AIzaSyDummy` with real Google Maps API key in `map.html`
3. Maps will show as blank but marker updates will work

### Issue: Shippers not moving

**Solution:**
- Ensure `auto-delivery` is started by clicking "Start Auto-Delivery" button
- Check that shipper has assigned orders (bottom log shows assignments)
- Verify pending order count shows > 0

### Issue: Thread not stopping cleanly

**Solution:** Close all shipper windows before closing main dashboard

## Performance Notes

- Handles 50+ orders comfortably with 4 shippers
- UI updates every 1 second (map refresh interval)
- Services run on separate threads, no UI blocking
- Memory usage: ~200MB for full system

## Future Enhancements

1. **Real Google Maps API** - Replace mock with actual API
2. **Database Backend** - Store orders and deliveries to DB
3. **REST API** - Expose services via HTTP endpoints
4. **WebSocket** - Real-time updates to web clients
5. **Route Optimization** - Use TSP algorithms for better routes
6. **Performance Metrics** - Average delivery time, shipper efficiency
7. **Authentication** - User login and role-based access

## Testing

To test end-to-end:

1. Run application
2. Observe orders appearing in log
3. Observe batches being created and assigned
4. Watch shippers receive assignments
5. Test manual delivery: Click "Deliver Next" on shipper app
6. Test auto delivery: Click "Start Auto-Delivery"
7. Verify map updates show shipper movement

## Contributing

This is a simulation/learning project. Feel free to extend:
- Add more sophisticated routing algorithms
- Implement priority-based order handling
- Add shipper ratings and preferences
- Create REST API wrapper
- Add metrics and analytics

## License

This project is provided as-is for educational purposes.

## Support

For issues or questions:
1. Check the log panel for error messages
2. Review console output for stack traces
3. Verify all dependencies are installed
4. Ensure Java 21+ is installed

---

**Happy delivering! 🚚📦**

