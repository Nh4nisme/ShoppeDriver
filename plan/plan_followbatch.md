# Plan Follow Batch

## Muc tieu

Hoan thien 2 nhom chuc nang tiep theo:
- theo doi batch da duoc gan cho shipper tren UI
- nhap dia chi co goi y theo kieu autocomplete de nhap lieu tu nhien hon

Chuc nang moi phai phu hop voi flow hien tai:
- tao batch
- gan batch
- shipper xem batch da gan
- admin theo doi lai batch va order trong batch

## Pham vi cong viec

### 1. Tao UI theo doi shipper va batch da gan

Muc tieu:
- Sau khi batch da duoc gan cho shipper, admin phai thay duoc danh sach shipper
- Khi nhan vao 1 shipper, UI phai hien:
  - batch da gan cho shipper do
  - danh sach order ben trong batch

Cong viec UI:
- Them mot khu vuc moi trong admin UI, co the la:
  - tab moi `Theo doi Batch`
  - hoac mo rong `ShipperStatusPanel`
- Hien danh sach shipper dang hoat dong
- Moi shipper item can co:
  - `shipperId`
  - `name`
  - `status`
  - batch hien tai neu co
- Khi chon shipper:
  - load batch hien tai cua shipper
  - hien chi tiet batch
  - hien danh sach order trong batch

Cong viec backend:
- Dam bao `ShipperTrackingService.getBatchesForShipper(...)` du dung
- Neu 1 shipper chi co 1 batch active:
  - co the them method ro rang hon, vi du `getActiveBatchForShipper(int shipperId)`
- `BatchRepository.findByShipperAndStatus(...)` can duoc kiem tra lai:
  - co load full orders
  - co dung cho `ASSIGNED`, `IN_DELIVERY`
- Co the can them method:
  - `List<Batch> findActiveByShipper(int shipperId)`

De xuat UI:
- Cot trai: danh sach shipper
- Cot phai: chi tiet batch cua shipper duoc chon
- Trong chi tiet batch hien:
  - `batchId`
  - `status`
  - `so order`
  - danh sach order

Rang buoc he thong:
- UI theo doi shipper khong duoc trung chuc nang voi `BatchAssignmentPanel`
- Can phan biet:
  - man hinh gan batch
  - man hinh theo doi batch da gan
- Sau khi assign batch thanh cong, panel theo doi phai refresh ngay

### 2. Them suggest dia chi khi nhap nhu Google Maps

Muc tieu:
- Khi nhap `From Address` va `To Address`, user thay duoc danh sach goi y dia chi
- User co the chon suggestion thay vi phai nhap full thu cong

Yeu cau:
- Neu goi API, phai dung API free
- Co the lay duoc ngay, khong doi dang ky phuc tap neu co the

Lua chon de xuat

#### Lua chon uu tien: Nominatim Search API

Vi sao:
- Free
- Co the goi ngay
- Khop voi huong dang dung OpenStreetMap

Can danh gia:
- rate limit
- user-agent requirement
- do chi tiet ket qua tai VN
- co autocomplete that su hay chi la search theo chuoi

#### Lua chon fallback: Photon API

Vi sao:
- free
- ho tro geocoding/autocomplete tot hon mot so truong hop
- phu hop lam suggestion nhanh

Can danh gia:
- do on dinh
- do phu ket qua tai HCM
- rate limit thuc te

Cong viec backend:
- Tao `AddressSuggestService`
- Method de xuat:
  - `List<AddressSuggestion> suggest(String keyword)`
- Tao model:
  - `AddressSuggestion`
  - field de xuat:
    - `displayText`
    - `lat`
    - `lng`
    - `rawAddress`
- Them cache memory:
  - `Map<String, List<AddressSuggestion>>`
- Debounce query de tranh spam API

Cong viec UI:
- Doi `TextField` thuong thanh autocomplete field
- Khi user nhap:
  - debounce 300-500ms
  - goi suggest API
  - render popup danh sach goi y
- Khi user chon 1 suggestion:
  - dien gia tri vao field
  - luu suggestion da chon
- Neu user sua text sau khi da chon suggestion:
  - reset suggestion state

Rang buoc ky thuat:
- Khong goi API tren moi keypress khong debounce
- Can xu ly:
  - input rong
  - API timeout
  - khong co ket qua
  - user nhap tay va khong chon suggestion

## Trinh tu thuc hien de xuat

1. Chot data flow theo doi batch da gan
- xac dinh nguon data shipper
- xac dinh nguon data active batch theo shipper
- chot panel UI moi

2. Hoan thien backend follow batch
- method lay batch active cua shipper
- method load orders theo batch
- refresh tracking data sau assign

3. Xay UI follow batch
- danh sach shipper
- click shipper de xem batch
- click/refresh de xem order trong batch

4. Chot API suggest dia chi
- uu tien thu Nominatim
- neu ket qua khong du tot, fallback sang Photon
- test voi dia chi Viet Nam, dac biet Ho Chi Minh

5. Xay `AddressSuggestService`
- call API
- parse response
- cache
- debounce-friendly

6. Gan autocomplete vao `BatchCreationPanel`
- `From Address`
- `To Address`
- xu ly state selected suggestion

7. Kiem thu full flow
- nhap dia chi bang suggest
- load orders
- tao batch
- gan batch cho shipper
- mo panel theo doi
- xem batch va order theo shipper

## Tieu chi hoan thanh

- Co UI hien danh sach shipper de theo doi batch da gan
- Nhan vao shipper se thay batch hien tai va cac order trong batch
- `From Address` va `To Address` co suggest khi nhap
- User co the chon suggestion thay vi nhap thu cong
- Flow tao batch va gan batch van chay thong suot

## Rui ro can chu y

- API suggest free co the bi rate limit
- ket qua suggest tai Viet Nam co the khong deu
- panel theo doi shipper co the bi stale data neu khong refresh dung luc
- shipper co batch `ASSIGNED` va `IN_DELIVERY` can duoc xu ly ro rang
- user co the nhap text khong chon suggestion, can quyet dinh co chap nhan hay bat buoc chon

## Ghi chu thiet ke

- Nen tach rieng:
  - geocode chot diem
  - suggest dia chi
- `GeoService` va `AddressSuggestService` khong nen tron logic vao nhau
- UI suggest can uu tien trai nghiem:
  - debounce
  - keyboard navigation
  - click chon nhanh
