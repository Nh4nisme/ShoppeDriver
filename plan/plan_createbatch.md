# Plan Create Batch

## Muc tieu

Hoan thien luong tao batch theo huong user-driven, cho phep admin:
- xem danh sach order tim duoc theo route
- chon tung order can dua vao batch
- tao batch dung voi cac order da chon
- xem chi tiet mot batch gom nhung order nao

Chuc nang `Create Batch` phai phu hop voi he thong hien tai, bao gom UI, service, repository, trang thai batch, va kha nang xem lai du lieu sau khi tao.

## Pham vi cong viec

### 1. Chinh `Load Orders` tu `TextArea` thanh checklist

Muc tieu:
- Khong chi preview dang text nua
- Hien thi danh sach order duoc tim thay duoi dang danh sach co the chon bo

Cong viec:
- Thay `TextArea` preview order trong `BatchCreationPanel` bang danh sach item co checkbox
- Moi item can hien thi it nhat:
  - `orderId`
  - `address`
  - toa do neu can debug
  - trang thai
- Sau khi bam `Load Orders`, UI phai:
  - goi route preview neu chua co route
  - goi `findOrdersAlongRoute(...)`
  - render danh sach order thanh checklist
- Them thao tac chon nhanh:
  - `Select All`
  - `Clear Selection`
- Neu khong co order:
  - hien trang thai rong ro rang
  - disable nut `Create Batch`

Rang buoc ky thuat:
- Khong duoc giu du lieu chi trong text preview
- Can co model tam thoi trong UI de track order da load va order duoc chon

### 2. Hoan thien `Create Batch` sau khi chon order

Muc tieu:
- Batch phai duoc tao tu danh sach order admin da tick
- Khong tao batch tu toan bo danh sach load duoc mot cach ngam dinh

Cong viec:
- Sua luong `Create Batch` de lay danh sach order da chon tu checklist
- Validate:
  - chua co route thi khong cho tao
  - chua load order thi khong cho tao
  - khong co order nao duoc chon thi khong cho tao
- Tao batch theo dung danh sach order selected
- Persist batch vao DB
- Cap nhat `orders.batch_id`
- Cap nhat `orders.status = IN_BATCH`
- Sau khi tao thanh cong:
  - thong bao `batchId`
  - refresh danh sach batch cho khu vuc assign/xem
  - reset selection neu hop ly

Cong viec backend can co:
- `BatchService.createBatch(List<Order> selectedOrders)` hoac method tuong duong phai la diem tao batch chinh
- `RouteBuilderService` nen tap trung vao route + load order, khong om toan bo logic UI selection
- Can xac dinh ro:
  - batch rong co duoc tao hay khong
  - duplicate order trong batch co bi chan hay khong
  - order dang `IN_BATCH` hoac status khac `PENDING` co bi loai tru hay khong

Rang buoc he thong:
- Chuc nang phai phu hop luong hien tai:
  - tao batch
  - assign shipper
  - shipper doc duoc order trong batch
- Khong duoc lam vo flow hien co ben `BatchAssignmentPanel`, `ShipperTrackingService`, `BatchRepository`

### 3. Them chuc nang xem batch va cac order trong batch

Muc tieu:
- Admin phai xem duoc batch da tao gom nhung don nao

Cong viec:
- Them khu vuc `View Batch` hoac mo rong panel batch hien co
- Hien danh sach batch da tao voi thong tin co ban:
  - `batchId`
  - `status`
  - `shipperId` neu da assign
  - so luong order
- Khi chon mot batch:
  - hien chi tiet cac order ben trong batch
  - thong tin order can co:
    - `orderId`
    - `address`
    - `status`
    - toa do neu can
- Neu co the, them filter:
  - `CREATED`
  - `ASSIGNED`
  - `IN_DELIVERY`
  - `COMPLETED`

Cong viec backend can co:
- `BatchRepository.findById(...)` hoac method tuong duong phai tra ve full orders
- Neu chua co, bo sung query load orders cua batch on-demand
- Dam bao UI view batch khong phu thuoc vao text log

## De xuat trinh tu thuc hien

1. Chuan hoa backend tao batch
- Chot `BatchService.createBatch(List<Order>)`
- Chot validate order hop le
- Chot repository save batch va assign order

2. Sua UI `Load Orders`
- doi preview text thanh checklist
- luu state selected orders
- them select all / clear

3. Sua UI `Create Batch`
- tao batch tu selected orders
- xu ly validation va trang thai nut bam

4. Bo sung `View Batch`
- danh sach batch
- chi tiet order trong batch
- refresh sau create va sau assign

5. Kiem thu full flow
- preview route
- load orders
- chon mot phan order
- tao batch
- xem lai batch vua tao
- assign shipper

## Tieu chi hoan thanh

Chuc nang duoc xem la hoan thanh khi:
- `Load Orders` hien checklist thay vi text area
- admin co the tick bo tung order
- `Create Batch` tao batch dung voi cac order duoc tick
- batch sau khi tao xem lai duoc day du danh sach order
- flow tao batch khong gay loi voi flow assign shipper va xem shipper hien tai

## Rui ro can chu y

- UI state va DB state khong dong bo sau khi tao batch
- order bi chon nhung da bi user khac dua vao batch truoc khi save
- batch list refresh chua kip sau khi tao
- panel xem batch va panel assign batch co the dang doc du lieu tu 2 nguon khac nhau
- can tranh tao batch rong hoac tao batch trung order

## Ghi chu thiet ke

- Nen tach ro:
  - route discovery
  - order loading
  - order selection
  - batch persistence
- Khong de UI tu quyet dinh logic nghiep vu qua nhieu
- `Create Batch` phai di qua service ro rang de sau nay mo rong duoc audit, validation, va permission
