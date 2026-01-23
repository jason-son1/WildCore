마인크래프트 1.21.1 야생 서버를 위한 자체 제작 플러그인 **`WildCore`**의 상세 개발 계획서입니다. 상점 기능을 제외하고, **주식, 인챈트, 유틸리티** 기능에 집중하여 **설정 파일(YAML) 기반**으로 작동하도록 설계했습니다.

이 문서는 개발자가 바로 코딩에 착수할 수 있도록 아키텍처, 데이터 구조, 핵심 로직을 구체화한 기술 명세서(Technical Specification)입니다.

---

## 1. 프로젝트 개요 (Project Overview)

- **플러그인 명:** `WildCore`
    
- **타겟 버전:** Minecraft Java Edition 1.21.1 (Paper API)
    
- **Java 버전:** Java 21 (JDK 21)
    
- **필수 의존성 (Dependencies):**
    
    - `Vault` (경제 연동)
        
    - `PlaceholderAPI` (변수 출력)
        
    - `Lombok` (개발 편의성 - 선택 사항)
        
- **핵심 철학:** **Data-Driven Design**. 모든 확률, 가격, 아이템, 메시지는 코드가 아닌 YAML 설정 파일에서 관리한다.
    

---

## 2. 패키지 및 클래스 구조 (Class Structure)

프로젝트는 유지보수가 용이하도록 기능별로 패키지를 분리합니다.

```
com.myserver.wildcore
├── WildCore.java              # 메인 클래스 (onEnable, onDisable)
├── config                     # 설정 관리
│   ├── ConfigManager.java     # 모든 YAML 파일 로드 및 캐싱
│   ├── StockConfig.java       # 주식 데이터 객체화
│   └── EnchantConfig.java     # 인챈트 데이터 객체화
├── managers                   # 핵심 로직
│   ├── StockManager.java      # 주식 가격 변동 및 타이머 스케줄러
│   └── EnchantManager.java    # 강화 확률 계산 및 결과 처리
├── gui                        # 인벤토리 UI
│   ├── StockGUI.java          # 주식 시장 UI 생성
│   └── EnchantGUI.java        # 강화소 UI 생성
├── listeners                  # 이벤트 감지
│   ├── GuiListener.java       # GUI 클릭 이벤트 처리
│   ├── PlayerListener.java    # 사망(세이브권), 상호작용(워프권) 처리
│   └── BlockListener.java     # 바닐라 인챈트 테이블 차단
└── commands                   # 명령어 처리
    └── MainCommand.java       # /wildcore reload, open, etc.
```

---

## 3. 데이터 및 설정 파일 설계 (Configuration)

이 플러그인의 핵심입니다. 운영자가 수정할 파일들의 구조입니다.

### A. `config.yml` (기본 설정)

YAML

```
prefix: "&8[&6WildCore&8] &f"
settings:
  stock_update_interval: 1800 # 초 단위 (30분)
  block_vanilla_enchant_table: true # 바닐라 인챈트 테이블 사용 금지 여부
messages:
  insufficient_funds: "&c돈이 부족합니다."
  insufficient_items: "&c재료 아이템이 부족합니다."
  enchant_success: "&a강화에 성공했습니다!"
  enchant_fail: "&c강화에 실패하여 재료가 소멸되었습니다."
  enchant_destroy: "&4강화 대실패! 아이템이 파괴되었습니다."
```

### B. `stocks.yml` (주식 시스템)

YAML

```
# 주식 GUI 설정
gui:
  title: "&8[ &a주식 시장 &8]"
  size: 27

# 종목 리스트
stocks:
  rotten_flesh_futures:
    display_name: "&2좀비 고기 선물"
    material: "ROTTEN_FLESH"
    slot: 10
    base_price: 500.0       # 기준가
    min_price: 100.0        # 하한가
    max_price: 2000.0       # 상한가
    volatility: 0.15        # 변동성 (15%)
    lore:
      - "&7현재 가격: &6%price%원"
      - "&7변동률: %change%"
```

### C. `enchants.yml` (인챈트 시스템)

YAML

```
gui:
  title: "&8[ &5강화소 &8]"
  size: 9

tiers:
  sharpness_1:
    display_name: "&b[ 날카로움 I 부여 ]"
    material: "ENCHANTED_BOOK"
    slot: 2
    
    # 적용 대상 아이템 (Material 이름)
    target_whitelist:
      - "DIAMOND_SWORD"
      - "IRON_SWORD"
      - "NETHERITE_SWORD"
    
    # 결과물 (인챈트:레벨)
    result:
      enchantment: "sharpness"
      level: 1
      
    # 비용
    cost:
      money: 1000.0
      items:
        - "DIAMOND:2"       # 다이아몬드 2개
        - "LAPIS_LAZULI:10" # 청금석 10개
    
    # 확률 (0~100)
    probability:
      success: 70.0  # 성공
      fail: 20.0     # 실패 (재료만 소멸)
      destroy: 10.0  # 파괴 (장비 소멸)
```

### D. `items.yml` (특수 아이템 정의)

이 파일에 정의된 아이템은 `/wildcore give <player> <id>`로 지급 가능하며, 시스템에서 인식합니다.

YAML

```
items:
  inventory_save_ticket:
    material: "PAPER"
    display_name: "&6[ 인벤토리 세이브권 ]"
    custom_model_data: 1001 # 텍스처팩 연동용
    lore:
      - "&7인벤토리에 소지하고 죽으면"
      - "&7아이템을 잃지 않습니다."
      - "&c(사용 시 1개 소모)"
      
  spawn_warp_ticket:
    material: "NETHER_STAR"
    display_name: "&b[ 스폰 워프권 ]"
    lore:
      - "&7우클릭 시 스폰으로 이동합니다."
```

---

## 4. 기능별 상세 로직 명세 (Logic Specs)

### [1] 주식 시스템 (Stock System)

**핵심 알고리즘:**

주식 가격 $P_{new}$는 이전 가격 $P_{old}$에 변동성 $V$를 적용하여 계산합니다.

$$P_{new} = P_{old} \times (1 + \text{Random}(-V, +V))$$

1. **데이터 관리:**
    
    - 서버 시작 시 `stocks.yml`을 로드하여 `Map<String, StockObject>`에 저장.
        
    - 유저의 주식 보유 현황은 `userdata/players.yml` 또는 SQLite에 `UUID: { stock_id: amount }` 형태로 저장.
        
2. **스케줄러 (Scheduler):**
    
    - `BukkitRunnable`을 사용하여 설정된 시간(`stock_update_interval`)마다 `updateStocks()` 실행.
        
    - 각 주식마다 난수를 발생시켜 가격 등락 결정 후, `min_price`와 `max_price` 사이 값으로 보정(Clamping).
        
3. **플레이스홀더 (PAPI) 연동:**
    
    - `%wildcore_stock_price_<stock_id>%`: 현재 가격 표시.
        
    - `%wildcore_stock_change_<stock_id>%`: 전 타임 대비 등락률 표시 (예: &c▼5%).
        

### [2] 인챈트 시스템 (Enchant System)

**프로세스 흐름:**

1. **진입:** 유저가 NPC 클릭 또는 `/wildcore enchant` 입력 -> `EnchantGUI` 오픈.
    
2. **검증:** 유저가 GUI 내의 강화 버튼 클릭.
    
    - **조건 1:** 손에 든 아이템이 `target_whitelist`에 포함되는가?
        
    - **조건 2:** `Vault`를 통해 소지금이 충분한가?
        
    - **조건 3:** 인벤토리에 `cost.items` 재료가 충분한가?
        
3. **실행:** 모든 조건 충족 시 재료/돈 차감 후 주사위 굴림 (`Math.random() * 100`).
    
    - **Range 0 ~ Success:** `item.addUnsafeEnchantment()` 실행 -> 성공 메시지 및 사운드.
        
    - **Range Success ~ (Success+Fail):** 아무 변화 없음 (재료만 소모) -> 실패 메시지.
        
    - **Range (Success+Fail) ~ 100:** `item.setAmount(0)` -> 아이템 파괴 메시지 및 폭발 파티클.
        

### [3] 유틸리티 시스템 (Utility System)

**A. 인벤토리 세이브권**

- **Event:** `PlayerDeathEvent`
    
- **Logic:**
    
    1. 사망한 플레이어의 인벤토리 전체 스캔.
        
    2. `items.yml`에 정의된 `inventory_save_ticket`과 일치하는 아이템(Display Name, Lore, Material 비교)이 있는지 확인.
        
    3. 있다면:
        
        - 아이템 스택 1개 감소.
            
        - `event.setKeepInventory(true)` 설정.
            
        - `event.getDrops().clear()` 설정.
            
        - `event.setDroppedExp(0)` 설정.
            
        - 플레이어에게 "세이브권이 사용되었습니다." 메시지 전송.
            

**B. 스폰 워프권**

- **Event:** `PlayerInteractEvent` (Action: RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK)
    
- **Logic:**
    
    1. 손에 든 아이템이 `spawn_warp_ticket`인지 확인.
        
    2. 맞다면:
        
        - 아이템 1개 감소.
            
        - `player.performCommand("spawn")` 실행 (EssentialsX 의존).
            

---

## 5. 개발 로드맵 (Development Roadmap)

이 순서대로 개발을 진행하면 의존성 문제를 최소화할 수 있습니다.

1. **Phase 1: 기본 골격 (Skeleton)**
    
    - 프로젝트 생성 (Gradle/Maven).
        
    - `plugin.yml` 작성 (명령어, 퍼미션, api-version: 1.21).
        
    - `ConfigManager` 구현: 파일 생성 및 읽기 테스트.
        
2. **Phase 2: 주식 시스템 (Stock)**
    
    - `StockManager` 구현: 가격 변동 알고리즘 작성.
        
    - `StockGUI` 구현: 아이콘 클릭 시 매수/매도 로직(채팅창에 숫자 입력 혹은 1개/64개 단위 구매).
        
    - PlaceholderAPI 확장 클래스 작성.
        
3. **Phase 3: 인챈트 시스템 (Enchant)**
    
    - `EnchantManager` 구현: 재료 체크 및 확률 계산 로직.
        
    - `EnchantGUI` 구현: 손에 든 아이템 감지 로직.
        
    - `items.yml` 파싱 로직 추가 (재료 아이템 인식을 위해).
        
4. **Phase 4: 유틸리티 및 통합 (Utilities & Integration)**
    
    - `PlayerListener` 구현: 세이브권, 워프권 로직.
        
    - `items.yml`의 커스텀 아이템 지급 명령어 구현.
        
    - 전체 테스트 및 디버깅.
        

## 6. 추가 제언 (Tips)

- **GUI 라이브러리:** 자체적으로 `InventoryHolder`를 구현하는 것이 학습에는 좋으나, 복잡한 GUI 처리를 위해 **`IF` (Inventory Framework)** 같은 경량 라이브러리를 음영(Shading) 처리하여 사용하는 것도 개발 속도를 높이는 방법입니다.
    
- **비동기 처리:** 주식 가격 업데이트나 DB(파일) 저장은 메인 스레드 렉(Lag)을 방지하기 위해 반드시 `runTaskAsynchronously`로 처리하되, Bukkit API 호출(아이템 지급 등)만 `runTask`로 메인 스레드로 돌아와야 합니다.
    
- **커스텀 아이템 매칭:** 단순히 이름만 비교하면 유저가 이름 변경 모루로 악용할 수 있습니다. 반드시 `PersistentDataContainer` (PDC)를 활용하여 아이템 내부에 보이지 않는 태그를 심어두고 이를 검사하는 방식을 권장합니다.