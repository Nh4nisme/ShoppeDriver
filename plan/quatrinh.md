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
