# Qua trinh thay doi code

## Prompt 1 - 2026-04-28

### Yeu cau
Tao file `quatrinh.md` ghi lai toan bo thay doi code sau moi prompt.

### Thay doi
- Tao moi file `quatrinh.md` tai thu muc goc cua project.
- Thiet lap mau ghi chep de theo doi cac thay doi code theo tung prompt.

### File bi anh huong
- `quatrinh.md`

### Ghi chu
- Tu prompt nay tro di, moi lan minh sua code se cap nhat file nay.

## Prompt 2 - 2026-04-28

### Yeu cau
Refactor luong tao batch va giao batch theo spec address-based route, repository changes, UI changes, bo auto loop.

### Thay doi
- Them `GeoService`, `RouteService`, `Route`, `LatLng` de geocode dia chi va tim route bang dich vu free, co cache memory.
- Refactor `RouteBuilderService` thanh API dong bo `createBatchFromAddresses(...)` va `previewRoute(...)`.
- Refactor `DispatcherService` bo run loop, them assign batch thu cong theo `batchId`/`shipperId`.
- Mo rong `Order` va cac repository de lam viec voi `address`, `district`, `latitude`, `longitude`, van giu tuong thich du lieu `x/y`.
- Sua `BatchRepository` de luu batch bang auto-increment va persist shipper assignment dung DB.
- Them `OrderService.findOrdersAlongRoute(...)` de loc orders cach route trong ban kinh 0.2 km.
- Cap nhat `BatchCreationPanel` tu nhap toa do sang nhap dia chi voi `Preview Route`, `Load Orders`, `Create Batch`.
- Dieu chinh `MainApp`, `ShipperTrackingService`, `db_schema.sql` cho flow moi.

### File bi anh huong
- `src/main/java/com/logistics/MainApp.java`
- `src/main/java/com/logistics/model/LatLng.java`
- `src/main/java/com/logistics/model/Order.java`
- `src/main/java/com/logistics/model/Route.java`
- `src/main/java/com/logistics/repository/BatchRepository.java`
- `src/main/java/com/logistics/repository/OrderRepository.java`
- `src/main/java/com/logistics/service/DispatcherService.java`
- `src/main/java/com/logistics/service/GeoService.java`
- `src/main/java/com/logistics/service/OrderService.java`
- `src/main/java/com/logistics/service/RouteBuilderService.java`
- `src/main/java/com/logistics/service/RouteService.java`
- `src/main/java/com/logistics/service/ShipperTrackingService.java`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `db_schema.sql`
- `quatrinh.md`

### Ghi chu
- `mvn -q -DskipTests compile` da pass.

## Prompt 9 - 2026-04-29

### Yeu cau
Code theo `plan_followbatch.md`.

### Thay doi
- Them backend cho suggest dia chi:
  - tao `AddressSuggestion`
  - tao `AddressSuggestService` voi cache memory
  - uu tien goi Nominatim, fallback sang Photon khi can
- Them backend theo doi batch theo shipper:
  - `BatchRepository.findActiveByShipper(...)`
  - `ShipperTrackingService.getActiveBatchForShipper(...)`
  - mo rong `getBatchesForShipper(...)`
- Them UI moi `FollowBatchPanel`:
  - hien danh sach shipper
  - nhan vao shipper de xem batch dang gan
  - hien danh sach order ben trong batch
- Tich hop `FollowBatchPanel` vao `Sidebar` thanh tab `Theo doi Batch`
- Tich hop autocomplete vao `BatchCreationPanel`:
  - debounce khi nhap
  - popup suggestion
  - chon suggestion de dien dia chi

### File bi anh huong
- `src/main/java/com/logistics/model/AddressSuggestion.java`
- `src/main/java/com/logistics/repository/BatchRepository.java`
- `src/main/java/com/logistics/service/AddressSuggestService.java`
- `src/main/java/com/logistics/service/ShipperTrackingService.java`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/java/com/logistics/ui/admin/FollowBatchPanel.java`
- `src/main/java/com/logistics/ui/admin/Sidebar.java`
- `plan/quatrinh.md`

### Ghi chu
- `mvn -q -DskipTests compile` da pass.

## Prompt 8 - 2026-04-29

### Yeu cau
Khong code, chi tao `plan_followbatch.md` trong folder `plan` cho 2 dau viec:
- tao UI theo doi shipper va batch da gan
- them suggest dia chi theo kieu autocomplete

### Thay doi
- Tao file `plan/plan_followbatch.md` mo ta ke hoach trien khai UI follow batch theo shipper va dia chi autocomplete bang API free.

### File bi anh huong
- `plan/plan_followbatch.md`
- `plan/quatrinh.md`

### Ghi chu
- Khong thay doi logic ung dung trong prompt nay.
- Network call toi Nominatim/OSRM chua duoc xac thuc trong moi truong hien tai.

## Prompt 3 - 2026-04-29

### Yeu cau
Rollback cac thay doi da ghi trong Prompt 2.

### Thay doi
- Khoi phuc cac file sua o Prompt 2 ve noi dung goc trong `HEAD`.
- Xoa cac file moi da duoc them o Prompt 2 la `Route.java` va `GeoService.java`.
- Dua `db_schema.sql` ve schema goc khong co `latitude/longitude`.

### File bi anh huong
- `src/main/java/com/logistics/MainApp.java`
- `src/main/java/com/logistics/model/LatLng.java`
- `src/main/java/com/logistics/model/Order.java`
- `src/main/java/com/logistics/repository/BatchRepository.java`
- `src/main/java/com/logistics/repository/OrderRepository.java`
- `src/main/java/com/logistics/service/DispatcherService.java`
- `src/main/java/com/logistics/service/OrderService.java`
- `src/main/java/com/logistics/service/RouteBuilderService.java`
- `src/main/java/com/logistics/service/ShipperTrackingService.java`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/java/com/logistics/model/Route.java`
- `src/main/java/com/logistics/service/GeoService.java`
- `db_schema.sql`
- `quatrinh.md`

### Ghi chu
- Giu nguyen `quatrinh.md` de lich su thay doi khong bi mat.

## Prompt 4 - 2026-04-29

### Yeu cau
Lam lai Prompt 2 theo spec address-based route, nhung van tuong thich voi schema DB hien tai.

### Thay doi
- Them `Route`, `GeoService`, `RouteService`, `BatchService` de geocode dia chi, lay route mien phi va tao batch theo danh sach orders.
- Refactor `RouteBuilderService` bo run loop, them `createBatchFromAddresses(String from, String to)` va `previewRoute(...)`.
- Refactor `DispatcherService` bo run loop, them `assignBatchToShipper(String batchId, String shipperId)`.
- Mo rong `Order` de mang `address` va helper `latitude/longitude` alias tren `x/y`.
- Mo rong `OrderRepository` voi `findByDistrictAndBoundingBox(...)`, mapping them `address`, va luu order co dia chi.
- Sua `BatchRepository` de luu batch bang auto-increment, them `findById(...)` va `assignToShipper(...)`.
- Them `OrderService.findOrdersAlongRoute(route, 0.2)` voi loc bounding box + khoang cach haversine.
- Cap nhat `ShipperTrackingService` de tra ve batch `CREATED` va `ASSIGNED`.
- Doi `BatchCreationPanel` tu input toa do sang input dia chi voi `Preview Route`, `Load Orders`, `Create Batch`.
- Cap nhat `MainApp` de khong con start `RouteBuilderService` va `DispatcherService` nhu background loop.

### File bi anh huong
- `src/main/java/com/logistics/MainApp.java`
- `src/main/java/com/logistics/model/LatLng.java`
- `src/main/java/com/logistics/model/Order.java`
- `src/main/java/com/logistics/model/Route.java`
- `src/main/java/com/logistics/repository/BatchRepository.java`
- `src/main/java/com/logistics/repository/OrderRepository.java`
- `src/main/java/com/logistics/service/BatchService.java`
- `src/main/java/com/logistics/service/DispatcherService.java`
- `src/main/java/com/logistics/service/GeoService.java`
- `src/main/java/com/logistics/service/OrderService.java`
- `src/main/java/com/logistics/service/RouteBuilderService.java`
- `src/main/java/com/logistics/service/RouteService.java`
- `src/main/java/com/logistics/service/ShipperTrackingService.java`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `quatrinh.md`

### Ghi chu
- Khong sua `db_schema.sql` trong vong nay de tranh lech schema DB thuc te.
- `mvn -q -DskipTests compile` da pass.

## Prompt 5 - 2026-04-29

### Yeu cau
Tang khoang cach tim kiem don gan route len ban kinh 5 km.

### Thay doi
- Doi nguong loc orders theo route tu `0.2 km` len `5.0 km` trong flow tao batch theo dia chi.
- Cap nhat thong bao UI cho khop voi ban kinh moi.

### File bi anh huong
- `src/main/java/com/logistics/service/RouteBuilderService.java`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `quatrinh.md`

### Ghi chu
- Khong sua `LocationUtil` vi do la logic cu khong nam trong flow address-based moi.

## Prompt 6 - 2026-04-29

### Yeu cau
Khong code, chi tao plan va ghi vao `plan_createbatch.md`.

### Thay doi
- Tao file `plan_createbatch.md` mo ta ke hoach hoan thien luong `Load Orders`, `Create Batch`, va `View Batch`.

### File bi anh huong
- `plan_createbatch.md`
- `quatrinh.md`

### Ghi chu
- Khong thay doi logic ung dung trong prompt nay.

## Prompt 7 - 2026-04-29

### Yeu cau
Thuc hien code theo `plan_createbatch.md`.

### Thay doi
- Hoan thien backend tao batch theo danh sach order duoc chon:
  - them `OrderRepository.findById(...)`
  - bo sung validate trong `BatchService.createBatch(...)` de chan batch rong, loc order trung, va tu choi order khong con `PENDING`
- Chuyen `Load Orders` trong `BatchCreationPanel` tu text preview sang checklist co the chon bo:
  - render tung order voi checkbox
  - them `Select All` va `Clear Selection`
  - theo doi state selected orders trong UI
- Hoan thien `Create Batch`:
  - tao batch tu danh sach order duoc tick
  - disable nut create khi khong co selection
  - refresh du lieu tracking sau khi tao thanh cong
- Mo rong `ShipperTrackingService` de ho tro refresh UI ngay va tra ve them cac batch `IN_DELIVERY`, `COMPLETED`
- Nang cap tab `Danh sach Batch` trong `Sidebar` thanh chuc nang xem batch:
  - loc batch theo status
  - chon 1 batch de xem chi tiet
  - hien danh sach order ben trong batch

### File bi anh huong
- `src/main/java/com/logistics/repository/OrderRepository.java`
- `src/main/java/com/logistics/service/BatchService.java`
- `src/main/java/com/logistics/service/ShipperTrackingService.java`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/java/com/logistics/ui/admin/Sidebar.java`
- `quatrinh.md`

### Ghi chu
- `mvn -q -DskipTests compile` da pass.

## Prompt 10 - 2026-04-29

### Yeu cau
1. Lam lai logic Route - lay orders tren duong di (a -> b), khong phai xung quanh ban kinh.
2. Chinh UI BatchCreationPanel - chia input dia chi thanh nhieu field rieng: ten duong, so nha, phuong/quan, tinh/thanh pho.
3. Them tinh nang chat Socket TCP giua shipper va admin.

### Thay doi
- **Route Logic Refactor:**
  - Sua `OrderService.findOrdersAlongRoute()` de tinh perpendicular distance tu moi order toi polyline segments.
  - Thay the haversine radius filtering (5 km) bang point-to-segment distance (~0.3-0.5 km).
  - Loc chi orders nam tren duong di thuc te, khong phuc vu orders xung quanh.

- **Address Input UI Split:**
  - Tach `createInputSection()` trong `BatchCreationPanel` thanh cau truc components:
    - Street name (ten duong)
    - Street number (so nha)
    - Ward/District (phuong/quan)
    - City/Province (tinh/thanh pho)
  - Mo rong `AddressSuggestion` model: them fields `street`, `number`, `ward`, `district`, `city`.
  - Cap nhat `GeoService.geocode()` de nhan structured address components va xay dung dia chi day du truoc khi geocode.
  - Giu autocomplete cho tung component hoac toan bo dia chi.

- **Shipper-Admin TCP Chat Feature:**
  - Tao model `ChatMessage` (serializable): shipperId, adminId, messageContent, timestamp.
  - Tao `ChatServer` (admin-side): TCP server tren port 9999, accept client connections, broadcast messages.
  - Tao `ChatClient` (shipper-side): TCP client, maintain persistent connection toi server.
  - Tao `ChatPanel` UI trong shipper app: message list + input field + connect status.
  - Tao `AdminChatPanel` UI trong admin app: shipper selector + chat area.
  - Them message queue vao `ShipperWorker` va ShipperTrackingService.
  - Them `ChatListener` interface hoac extend `DataChangeListener` voi `onChatMessageReceived()`.
  - Integrate ChatClient init vao `ShipperApp` startup.
  - Integrate ChatServer init vao `MainApp` startup.

### File bi anh huong
- `src/main/java/com/logistics/model/AddressSuggestion.java`
- `src/main/java/com/logistics/model/ChatMessage.java` **(NEW)**
- `src/main/java/com/logistics/service/OrderService.java`
- `src/main/java/com/logistics/service/GeoService.java`
- `src/main/java/com/logistics/chat/ChatServer.java` **(NEW)**
- `src/main/java/com/logistics/chat/ChatClient.java` **(NEW)**
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/java/com/logistics/ui/shipper/ChatPanel.java` **(NEW)**
- `src/main/java/com/logistics/ui/admin/AdminChatPanel.java` **(NEW)**
- `src/main/java/com/logistics/util/DataChangeListener.java`
- `src/main/java/com/logistics/worker/ShipperWorker.java`
- `src/main/java/com/logistics/service/ShipperTrackingService.java`
- `src/main/java/com/logistics/shipper/ShipperApp.java`
- `src/main/java/com/logistics/MainApp.java`
- `plan/quatrinh.md`

### Ghi chu
- Route filtering: Su dung perpendicular distance calculation (chinh xac hon cho "on-route" detection vs radius).
- Address validation: Keep flexible entry cho MVP; add DB validation trong future iteration.
- Chat persistence: Session-only cho MVP; persist to DB va implement shipper-admin routing trong future.
- Chat thread safety: Use CopyOnWriteArrayList cho listeners, BlockingQueue cho message passing.
- Heartbeat mechanism: Recommend ping/pong frames de detect stale TCP connections.

## Prompt 11 - 2026-04-29 - IMPLEMENTATION

### Yeu cau
Thuc hien 3 feature: Route logic fix, Address UI split, va TCP chat.

### Thay doi
- **Route Logic Refactor - DONE:**
  - Sua `OrderService.findOrdersAlongRoute()` tinh perpendicular distance tu order toi polyline segments.
  - Cap nhat `BatchCreationPanel` threshold tu 5.0 km sang 0.5 km.
  - Them helper methods: `calculateMinDistanceToPolyline()` va `perpendicularDistanceToSegment()`.

- **Address Input UI Split - DONE:**
  - Tach `createInputSection()` thanh separate component fields: street, number, ward, district, city.
  - Tao `createAddressComponentBox()` method de xay dung structured address section.
  - Mo rong `AddressSuggestion` model: them fields `street`, `number`, `ward`, `district`, `city`.
  - Cap nhat `GeoService`: them `geocodeStructured()` method nhan components va xay dung full address.
  - Cap nhat `RouteBuilderService`: them getter methods cho `geoService` va `routeService`.
  - Cap nhat `previewRoute()` va `loadOrders()` de su dung structured fields va geoService.geocodeStructured().
  - Xoa autocomplete methods khong con su dung.

- **Shipper-Admin TCP Chat - DONE:**
  - Tao `ChatMessage` model (serializable): shipperId, senderName, messageContent, timestamp, isFromAdmin.
  - Tao `ChatServer`: TCP server tren port 9999, accept multiple client connections, broadcast messages.
  - Tao `ChatClient`: TCP client, maintain persistent connection toi server, reconnect logic.
  - Tao `ChatPanel` UI trong shipper app: message area, input field, connection status.
  - Tao `AdminChatPanel` UI trong admin app: shipper selector, chat area, message display.
  - Integrate `ChatServer.start()` vao `MainApp.initializeServices()`.
  - Integrate `ChatServer.stop()` vao `MainApp.shutdown()`.
  - Integrate `ChatClient` vao `ShipperApp`: initialize, connect on startup, disconnect on close.
  - Them listener pattern cho chat events.

### File bi anh huong
- `src/main/java/com/logistics/model/AddressSuggestion.java` - Extended voi address components
- `src/main/java/com/logistics/model/ChatMessage.java` - NEW, serializable chat message model
- `src/main/java/com/logistics/service/OrderService.java` - Refactored route filtering logic
- `src/main/java/com/logistics/service/GeoService.java` - Them geocodeStructured() method
- `src/main/java/com/logistics/service/RouteBuilderService.java` - Them getter methods
- `src/main/java/com/logistics/chat/ChatServer.java` - NEW, TCP server for admin
- `src/main/java/com/logistics/chat/ChatClient.java` - NEW, TCP client for shipper
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java` - Refactored UI, new address fields
- `src/main/java/com/logistics/ui/shipper/ChatPanel.java` - NEW, shipper chat UI
- `src/main/java/com/logistics/ui/admin/AdminChatPanel.java` - NEW, admin chat UI
- `src/main/java/com/logistics/MainApp.java` - Integrated ChatServer startup/shutdown
- `src/main/java/com/logistics/shipper/ShipperApp.java` - Integrated ChatClient init/connect/disconnect
- `plan/quatrinh.md`

### Ghi chu
- `mvn -q -DskipTests compile` - Need to verify
- Route perpendicular distance calculation su dung haversine formula cho segment endpoints va point.
- Chat use simple text protocol: `SHIPPER_ID:id:name` for identification, `MSG:senderName:message` for messages.
- Chat server multi-threaded: moi shipper client co 1 ClientHandler thread.
- Shipper address thanh structured component fields - UI ghi seo va highlight component validation trong future.
- Address geocoding reconstruct full address string tu components truoc goi Nominatim API.

## Prompt 12 - 2026-04-29

### Yeu cau
- Sua lai form nhap dia chi de label hien thi day du, form than thien hon.
- Bat lai goi y dia chi khi nguoi dung dang nhap chu.
- Mo app fullscreen mac dinh.
- Ghi lai thay doi vao `plan/quatrinh.md`.
- Them log bang `Logger`.

### Thay doi
- **Autocomplete + UI form dia chi:**
  - Khoi phuc autocomplete trong `BatchCreationPanel` sau khi tach field.
  - Goi y duoc kich hoat khi go tren cac field dia chi va popup hien duoi o dang nhap.
  - Khi chon 1 goi y, form tu dien `street`, `number`, `ward`, `district`, `city`.
  - Chinh lai layout `createAddressComponentBox()` sang dang form than thien hon, label rong hon va khong bi cat chu.
  - Them helper text de nguoi dung biet co the go chu de lay goi y.
- **AddressSuggestService parsing:**
  - Mo rong parse response Nominatim/Photon de map duoc cac truong cau truc (`street`, `number`, `ward`, `district`, `city`) vao `AddressSuggestion`.
- **Fullscreen mac dinh:**
  - Admin app mo voi `setMaximized(true)` va `setFullScreen(true)`.
  - Shipper app cung mo fullscreen mac dinh.
- **Them log theo Logger:**
  - `Logger` duoc day them vao `LogPanel` khi UI da san sang.
  - Them log cho cac action chinh trong `BatchCreationPanel`: preview route, load orders, create batch, suggest address, chon suggestion, va loi UI.
  - Them log cho fullscreen/open app va startup/shutdown service.

### File bi anh huong
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/java/com/logistics/service/AddressSuggestService.java`
- `src/main/java/com/logistics/util/Logger.java`
- `src/main/java/com/logistics/MainApp.java`
- `src/main/java/com/logistics/shipper/ShipperApp.java`
- `plan/quatrinh.md`

### Ghi chu
- Goi y dia chi hien dang dua tren keyword duoc ghep tu cac field da nhap trong tung section dia chi.
- Logger van ghi `stdout` nhu cu, dong thoi co gang day log len `LogPanel` neu JavaFX UI da khoi tao.

## Prompt 13 - 2026-04-29

### Yeu cau
- Khi bam `Preview Route`, tuyen duong can hien ngay tren map preview.
- Tang chieu cao phan `createOrderSection`.
- Tang do rong field `So nha`.
- Tang width cua `BatchCreationPanel`.
- Khong ghi log o panel ben duoi nua, chi ghi ra ngoai.

### Thay doi
- **Preview route len map:**
  - Mo rong `GoogleMapsPanel` de giu `previewRoute` va push polyline xuong `map.html`.
  - Them route overlay + marker `A/B` trong `map.html`.
  - `BatchCreationPanel.previewRoute()` gio goi `GoogleMapsPanel.showRoutePreview(route)` ngay sau khi route tra ve.
  - Sau khi tao batch thanh cong, preview route tren map duoc clear.
- **Resize UI tao batch:**
  - Tang `Sidebar` width de tab `Tao Batch` thoang hon.
  - Tang `BatchCreationPanel` width.
  - Tang `So nha` field width de nhap de hon.
  - Tang `ScrollPane` cua `createOrderSection` de khu vuc order cao hon.
- **Bo log panel duoi dashboard:**
  - `Logger` chi con ghi ra `stdout`.
  - `DashboardView` khong con gan `LogPanel` o bottom.
  - Xoa cac call `logPanel.log(...)` trong `MainApp`.

### File bi anh huong
- `src/main/java/com/logistics/ui/GoogleMapsPanel.java`
- `src/main/resources/map.html`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/java/com/logistics/ui/admin/Sidebar.java`
- `src/main/java/com/logistics/ui/admin/DashboardView.java`
- `src/main/java/com/logistics/MainApp.java`
- `src/main/java/com/logistics/util/Logger.java`
- `plan/quatrinh.md`

### Ghi chu
- Preview route duoc fit bounds theo polyline, uu tien xem tuyen dang preview.
- Log panel class van con trong codebase nhung khong con duoc su dung tren dashboard.

## Prompt 14 - 2026-04-29

### Yeu cau
- Hien thi lai `LogPanel` o duoi, nhung chi cho log nghiep vu trong app.
- Log ngoai app van ghi ra terminal nhu cu.
- Co gang toi da de map preview route hien duoc khi search.

### Thay doi
- **LogPanel selective:**
  - Hien thi lai `LogPanel` o bottom cua `DashboardView`.
  - `Logger` van chi ghi ra console/terminal, khong day toan bo log vao UI.
  - Them `appLog(...)` trong `MainApp` va `BatchCreationPanel` de chi ghi nhung log nghiep vu can xem trong app:
    - dashboard san sang
    - background service start
    - preview route
    - load orders
    - create batch thanh cong/that bai
    - shutdown app
- **Map route preview stability:**
  - Thay `map.html` tu Google Maps key gia sang Leaflet + OpenStreetMap tiles.
  - Them co che `pendingMapData` de neu map chua init xong thi du lieu van duoc apply lai sau.
  - Route preview duoc render bang polyline Leaflet, kem marker A/B.
  - `GoogleMapsPanel` bo hardcode bounds 0..100, chi day du lieu shipper/order/previewRoute that.
  - Khi co preview route, map fit bounds theo route de nguoi dung thay tuyen uu tien.

### File bi anh huong
- `src/main/resources/map.html`
- `src/main/java/com/logistics/ui/GoogleMapsPanel.java`
- `src/main/java/com/logistics/ui/admin/DashboardView.java`
- `src/main/java/com/logistics/MainApp.java`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/java/com/logistics/util/Logger.java`
- `plan/quatrinh.md`

### Ghi chu
- Ban do gio khong phu thuoc Google Maps API key dummy nua.
- `LogPanel` duoi dashboard chi dung cho log nghiep vu nguoi dung can theo doi trong UI.

## Prompt 15 - 2026-04-29

### Yeu cau
- Map dang loi, can dung API/dich vu free khac de load map va route on dinh hon.
- `Preview Route` va `Load Orders` hien dang khong chay duoc, can tang do ben.

### Thay doi
- **Bundle map library local:**
  - Download `leaflet.js` va `leaflet.css` vao `src/main/resources/vendor/leaflet`.
  - Sua `map.html` de load Leaflet tu local resources thay vi CDN, giam rui ro WebView bi loi script ben ngoai.
- **Geocode fallback:**
  - `GeoService` khong con phu thuoc duy nhat vao Nominatim.
  - Neu Nominatim loi/khong co ket qua, fallback sang Photon.
- **Route fallback:**
  - `RouteService` them fallback route backend:
    - `router.project-osrm.org`
    - `routing.openstreetmap.de/routed-car`
  - Neu backend dau loi thi tu dong thu backend tiep theo.

### File bi anh huong
- `src/main/resources/map.html`
- `src/main/resources/vendor/leaflet/leaflet.js`
- `src/main/resources/vendor/leaflet/leaflet.css`
- `src/main/java/com/logistics/service/GeoService.java`
- `src/main/java/com/logistics/service/RouteService.java`
- `plan/quatrinh.md`

### Ghi chu
- Tile map van dung OpenStreetMap public tiles.
- Muc tieu vong nay la tranh diem hong do CDN/script map va geocode/route provider don le.

## Prompt 16 - 2026-04-29

### Yeu cau
- Doc ky project va fix lai toan bo flow map, preview orders, preview route.
- Dung map free on dinh.
- Ho tro multi-route giong Google Maps sau khi bam `Preview Route`.

### Thay doi
- **Phan tich loi goc:**
  - Map cu bi phu thuoc vao contract JS mong manh (`executeScript` nhung JSON vao string) va de race condition voi WebView load state.
  - Preview orders truoc do chua bao gio duoc day len map; map chi render orders thuoc cac batch da luu.
  - Route service chi lay 1 tuyen duy nhat, khong co state cho route alternatives.
- **Route alternatives:**
  - `RouteService` them `getAlternativeRoutes(...)`.
  - Goi OSRM voi `alternatives=true` va lay toi da 3 route options.
  - Sap xep route theo `duration`, sau do `distance`.
- **Map state refactor:**
  - Viet lai `GoogleMapsPanel` de quan ly:
    - `previewRoutes`
    - `selectedPreviewRouteIndex`
    - `previewOrders`
  - Them queue state khi WebView chua load xong va flush lai sau `Worker.State.SUCCEEDED`.
  - Day data xuong JS theo object JSON truc tiep thay vi nhung vao string JS.
- **Map render rewrite:**
  - Viet lai `map.html` bang Leaflet local resource.
  - Render rieng:
    - shipper markers
    - persisted batch orders
    - preview orders
    - multiple preview routes
    - marker A/B cho selected route
  - Tu dong fit bounds theo selected route hoac preview orders.
- **BatchCreationPanel multi-route UX:**
  - Them danh sach route options.
  - Click route trong UI -> update selected route -> highlight route tren map.
  - `Preview Route` gio se load nhieu route.
  - `Load Orders` gio se:
    - dung route duoc chon
    - day preview orders len map ngay
  - Clear route/orders preview sau khi tao batch xong.

### File bi anh huong
- `src/main/java/com/logistics/service/RouteService.java`
- `src/main/java/com/logistics/ui/GoogleMapsPanel.java`
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/resources/map.html`
- `plan/quatrinh.md`

### Ghi chu
- Du lieu DB hien tai da dung lat/lng that cho orders, nen preview orders co the hoat dong sau khi map-state duoc noi lai dung.
- `mvn test` da pass sau khi refactor.

## Prompt 17 - 2026-04-29

### Yeu cau
- Them thanh keo cho `createOrderSection`.
- Sau khi `Load Orders`, marker van hien sai cho.
- Map can zoom in / zoom out muot hon.

### Phan tich loi
- DB dang co 2 nhom shipper:
  - shipper that o TP.HCM
  - shipper random cu o toa do `42..57`
- Map truoc do van render tat ca shipper hop le theo kieu lat/lng chung chung, nen viewport de bi fit sai vung.
- `fitBounds(...)` dang bi goi lai lien tuc moi lan `updateMap()` (moi giay), nen nguoi dung vua zoom/pan la map bi snap lai ngay.

### Thay doi
- **UI keo duoc:**
  - Dung `SplitPane` theo chieu doc trong `BatchCreationPanel` de co thanh keo giua input section va order section.
  - `ScrollPane` cua order list set `VbarPolicy.ALWAYS` de co thanh cuon ro rang.
- **Fix marker sai cho:**
  - `GoogleMapsPanel` loc shipper/order theo bounds toa do Viet Nam (`lat 8..24`, `lng 102..110`) de bo qua cac shipper random cu.
- **Fix zoom muot:**
  - `map.html` khong con auto-fit moi giay.
  - Them `lastFitSignature` de chi fit lai khi route/preivew orders thay doi that su.
  - Sau khi fit 1 lan, nguoi dung co the zoom/pan muot ma khong bi reset lien tuc.

### File bi anh huong
- `src/main/java/com/logistics/ui/admin/BatchCreationPanel.java`
- `src/main/java/com/logistics/ui/GoogleMapsPanel.java`
- `src/main/resources/map.html`
- `plan/quatrinh.md`

### Ghi chu
- Day la fix truc tiep cho van de viewport va marker rendering sai vi du lieu shipper cu.

## Prompt 18 - 2026-05-01

### Yeu cau
- Tao Shipper login UI va launcher, integrate ShipperApp voi login
- Chat TCP server/client da ton tai - tiep tuc hoan thien
- Ghi log thay doi vao `plan/quatrinh.md`

### Thay doi
- Them `ShipperLoginLauncher` la mot Swing-based launcher cho man hinh dang nhap shipper.
  - Su dung `AuthRepository` va `DBConnection` de xac thuc username/password.
  - Chi cho phep user co `role = SHIPPER` dang nhap.
  - Neu dang nhap thanh cong thi goi `ShipperApp.main(<shipperId>)` de mo app shipper.
- Khong thay doi cac file server/chat da ton tai (ChatServer, ChatClient, ChatMessage), su dung lai cac lop da co.

### File bi anh huong
- `src/main/java/com/logistics/shipper/ShipperLoginLauncher.java` (NEW)
- `plan/quatrinh.md` (append log)

### Ghi chu
- Launcher su dung Swing de don gian hoa viec dang nhap truoc khi khoi tao JavaFX `ShipperApp`.
- Da re-use `AuthRepository`, `DatabaseInitializer`, `User` va `Logger` tu code hien co.
- De chay: `java -cp target/classes;path\to\javafx libs com.logistics.shipper.ShipperLoginLauncher` (login dialog se hien thi va goi `ShipperApp`).

## Prompt 19 - 2026-05-01

### Yeu cau
- Chinh sua UI cua `ShipperApp` theo kieu mobile-like va them cot ben trai hien thi danh sach batch.

### Thay doi
- Cap nhat `ShipperApp.createUI()`:
  - Them `batchListView` o cot ben trai (ListView) de hien thi cac batch dang active cua shipper.
  - Dieu chinh layout thanh mobile-like: header nho, center lam main order list, right la chi tiet + chat.
  - Khi chon mot batch tren danh sach thi load batch do vao UI chi tiet.
- Cap nhat `startBatchPolling()`/`checkForNewBatch()` de refresh `batchListView` tu `BatchRepository.findActiveByShipper(shipperId)`.

### File bi anh huong
- `src/main/java/com/logistics/shipper/ShipperApp.java` (UI update, batch list)
- `plan/quatrinh.md`

### Ghi chu
- Khi chay app, cot ben trai se hien cac batch active (ASSIGNED/IN_DELIVERY/COMPLETED). Neu rong thi hien placeholder.
- Su dung lai `BatchRepository.findActiveByShipper(...)` de cap nhat danh sach.

## Prompt 20 - 2026-05-01

### Yeu cau
- Thu nho cua so cua `ShipperApp` khong full screen, chi mo o kich thuoc nho hon (khoang nua man hinh) de giao dien gon hon.

### Thay doi
- Cap nhat `ShipperApp.createUI()`:
  - Bo cac goi `setMaximized(true)` va `setFullScreen(true)`.
  - Dat kich thuoc scene va stage thanh 800x700 va centerOnScreen().
  - De resizable = true de nguoi dung co the thay doi kich thuoc tuong ung.

### File bi anh huong
- `src/main/java/com/logistics/shipper/ShipperApp.java`
- `plan/quatrinh.md`

### Ghi chu
- Muc tieu: GUI shipper hien nho gon hon va tu nhien hon so voi admin fullscreen. Van giu layout mobile-like va cot ben trai hien thi batch.




