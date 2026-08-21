# SUMO 交通仿真完整使用手册

## 目录
1. [SUMO 简介](#一sumo-简介)
2. [安装指南](#二安装指南)
3. [快速入门：单独使用 SUMO](#三快速入门单独使用-sumo)
4. [进阶应用：结合真实地图数据](#四进阶应用结合真实地图数据)
5. [大规模车辆生成](#五大规模车辆生成)
6. [与 Spring Boot 集成](#六与-spring-boot-集成)
7. [常见问题与解决方案](#七常见问题与解决方案)

---

## 一、SUMO 简介

### 1.1 什么是 SUMO？

SUMO（Simulation of Urban MObility）是一个**开源、微观、多模态的交通仿真工具/软件包**。它同时具备：

- **工具属性**：可直接运行的交通仿真程序
- **框架属性**：提供 TraCI 接口，支持二次开发
- **平台属性**：包含完整的工具生态系统

### 1.2 核心组件

| 组件 | 功能 |
|------|------|
| `sumo` | 命令行仿真工具 |
| `sumo-gui` | 图形界面仿真工具 |
| `netconvert` | 路网转换工具 |
| `netedit` | 路网编辑器 |
| `polyconvert` | 多边形转换工具 |
| `duarouter` | 动态路径分配 |
| `randomTrips.py` | 随机车辆生成 |

### 1.3 核心概念

- **路网文件（.net.xml）**：定义道路、路口、车道
- **需求文件（.rou.xml）**：定义车辆、路线、出发时间
- **配置文件（.sumocfg）**：整合所有仿真参数

---

## 二、安装指南

### 2.1 macOS Monterey 安装

#### 方法 1：Homebrew 安装（推荐）

```bash
# 1. 安装 Homebrew（如果未安装）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. 安装 SUMO
brew update
brew install sumo

# 3. 验证安装
sumo --version
netconvert --version

# 4. 如果找不到命令，添加 PATH
echo 'export PATH="/opt/homebrew/opt/sumo/bin:$PATH"' >> ~/.zshrc
echo 'export SUMO_HOME="/opt/homebrew/opt/sumo/share/sumo"' >> ~/.zshrc
source ~/.zshrc
```

#### 方法 2：下载 DMG 安装包

```bash
# 1. 访问 https://sumo.dlr.de/docs/Downloads.php
# 2. 下载 macOS 版本的 .dmg 文件
# 3. 双击安装，将 SUMO 拖入 Applications
# 4. 配置环境变量
echo 'export PATH="/Applications/SUMO.app/Contents/Resources/bin:$PATH"' >> ~/.zshrc
echo 'export SUMO_HOME="/Applications/SUMO.app/Contents/Resources"' >> ~/.zshrc
source ~/.zshrc
```

### 2.2 Linux 安装

```bash
# Ubuntu/Debian
sudo add-apt-repository ppa:sumo/stable
sudo apt-get update
sudo apt-get install sumo sumo-tools sumo-doc
```

### 2.3 Windows 安装

```powershell
# 1. 下载 Windows 安装包（.msi）
# 2. 运行安装程序
# 3. 添加系统环境变量 PATH
# 4. 验证
sumo --version
```

---

## 三、快速入门：单独使用 SUMO

### 3.1 创建第一个仿真

#### 步骤 1：创建工作目录

```bash
mkdir ~/sumo_tutorial
cd ~/sumo_tutorial
```

#### 步骤 2：创建路网文件

创建 `simple.net.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<net version="1.9" junctionCornerDetail="5" limitTurnSpeed="5.50"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
     xsi:noNamespaceSchemaLocation="http://sumo.dlr.de/xsd/net_file.xsd">

    <!-- 路口 -->
    <junction id="center" type="priority" x="0" y="0" 
              incLanes="bottom_right top_left right_bottom left_top" 
              intLanes="" shape="0,0 0,0"/>
    
    <!-- 四条道路的边 -->
    <edge id="bottom" from="center" to="center" priority="-1">
        <lane id="bottom_right" index="0" speed="13.89" length="100" 
              shape="0,0 0,-100"/>
    </edge>
    
    <edge id="top" from="center" to="center" priority="-1">
        <lane id="top_left" index="0" speed="13.89" length="100" 
              shape="0,0 0,100"/>
    </edge>
    
    <edge id="right" from="center" to="center" priority="-1">
        <lane id="right_bottom" index="0" speed="13.89" length="100" 
              shape="0,0 100,0"/>
    </edge>
    
    <edge id="left" from="center" to="center" priority="-1">
        <lane id="left_top" index="0" speed="13.89" length="100" 
              shape="0,0 -100,0"/>
    </edge>
</net>
```

#### 步骤 3：创建车辆需求文件

创建 `simple.rou.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<routes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
        xsi:noNamespaceSchemaLocation="http://sumo.dlr.de/xsd/routes_file.xsd">
    
    <!-- 定义车辆类型 -->
    <vType id="car" accel="2.6" decel="4.5" sigma="0.5" length="5" 
           maxSpeed="13.89" minGap="2.5"/>
    
    <!-- 定义路线 -->
    <route id="route1" edges="bottom top"/>
    <route id="route2" edges="right left"/>
    <route id="route3" edges="top bottom"/>
    <route id="route4" edges="left right"/>
    
    <!-- 定义车辆 -->
    <vehicle id="veh1" type="car" depart="0" route="route1"/>
    <vehicle id="veh2" type="car" depart="2" route="route2"/>
    <vehicle id="veh3" type="car" depart="4" route="route3"/>
    <vehicle id="veh4" type="car" depart="6" route="route4"/>
</routes>
```

#### 步骤 4：创建配置文件

创建 `simple.sumocfg`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
               xsi:noNamespaceSchemaLocation="http://sumo.dlr.de/xsd/sumoConfiguration.xsd">
    <input>
        <net-file value="simple.net.xml"/>
        <route-files value="simple.rou.xml"/>
    </input>
    <time>
        <begin value="0"/>
        <end value="100"/>
    </time>
    <output>
        <tripinfo-output value="tripinfo.xml"/>
    </output>
</configuration>
```

#### 步骤 5：运行仿真

```bash
# 命令行运行（无界面）
sumo -c simple.sumocfg

# GUI 运行（可视化）
sumo-gui -c simple.sumocfg
```

**GUI 操作技巧：**
- **空格键**：暂停/继续
- **Ctrl+P**：截图
- **鼠标滚轮**：缩放
- **右键拖动**：平移视图

### 3.2 生成随机路网

```bash
# 生成 5x5 网格路网
netgenerate --grid \
  --grid.number=5 \
  --grid.length=200 \
  --output-file=grid.net.xml

# 生成随机蜘蛛网路网
netgenerate --spider \
  --spider.arm-number=8 \
  --spider.circle-number=2 \
  --output-file=spider.net.xml
```

### 3.3 使用 netedit 编辑路网

```bash
# 打开路网编辑器
netedit simple.net.xml

# 常用操作：
# - 添加道路：选择 Edge 模式，点击添加
# - 添加路口：选择 Junction 模式
# - 添加信号灯：选择 Traffic Light 模式
# - 保存：Ctrl+S
```

---

## 四、进阶应用：结合真实地图数据

### 4.1 从 OpenStreetMap 获取真实路网

#### 方法 A：网站下载

1. 访问 [OpenStreetMap](https://www.openstreetmap.org/)
2. 导航到目标区域
3. 点击"导出"按钮
4. 选择范围，导出为 `.osm` 文件

#### 方法 B：命令行下载

```bash
# 下载北京 CBD 区域（示例坐标）
wget -O beijing.osm "https://api.openstreetmap.org/api/0.6/map?bbox=116.35,39.90,116.45,39.95"

# 下载上海陆家嘴区域
wget -O shanghai.osm "https://api.openstreetmap.org/api/0.6/map?bbox=121.49,31.23,121.51,31.25"

# 使用 curl 下载
curl -o city.osm "https://api.openstreetmap.org/api/0.6/map?bbox=116.30,39.85,116.50,40.00"
```

**坐标格式说明：**
- bbox = 最小经度,最小纬度,最大经度,最大纬度
- 经度范围：-180 到 180
- 纬度范围：-90 到 90

#### 方法 C：使用 Overpass API（大区域）

```bash
# 下载更大范围的数据
wget -O city_large.osm "http://overpass-api.de/api/map?bbox=116.30,39.85,116.50,40.00"
```

### 4.2 转换 OSM 为 SUMO 路网

#### 基本转换

```bash
# 最简单的转换
netconvert --osm-files beijing.osm -o beijing.net.xml

# 查看转换过程
netconvert --osm-files beijing.osm -o beijing.net.xml --verbose
```

#### 高级转换（推荐）

```bash
# 保留主要道路，简化路网
netconvert --osm-files beijing.osm \
  --output-file beijing.net.xml \
  --geometry.remove \
  --roundabouts.guess \
  --ramps.guess \
  --junctions.join \
  --tls.guess-signals \
  --tls.discard-simple \
  --tls.join \
  --remove-edges.isolated \
  --keep-edges.by-type highway.motorway,highway.trunk,highway.primary,highway.secondary
```

**参数说明：**
- `--geometry.remove`：简化几何
- `--roundabouts.guess`：识别环岛
- `--ramps.guess`：识别匝道
- `--junctions.join`：合并路口
- `--tls.guess-signals`：猜测信号灯
- `--remove-edges.isolated`：移除孤立道路
- `--keep-edges.by-type`：保留指定道路类型

### 4.3 使用 OSMWebWizard（GUI 工具）

```bash
# 启动 OSM Web 向导
osmWebWizard.py

# 指定区域启动
osmWebWizard.py --bbox="116.35,39.90,116.45,39.95"
```

**使用步骤：**
1. 浏览器自动打开
2. 选择地图区域
3. 设置车辆类型和数量
4. 点击生成
5. 自动创建所有文件并启动仿真

### 4.4 自动化脚本：完整工作流

创建 `create_real_network.py`：

```python
import os
import subprocess
import requests
from typing import Tuple

class RealNetworkCreator:
    """从真实地图创建 SUMO 路网的自动化工具"""
    
    def __init__(self, sumo_home: str = "/opt/homebrew/opt/sumo/share/sumo"):
        self.sumo_home = sumo_home
        self.sumo_tools = os.path.join(sumo_home, "tools")
        
    def download_osm_data(self, bbox: Tuple[float, float, float, float], 
                         output_file: str) -> bool:
        """下载 OSM 数据"""
        min_lon, min_lat, max_lon, max_lat = bbox
        
        url = f"https://api.openstreetmap.org/api/0.6/map?bbox={min_lon},{min_lat},{max_lon},{max_lat}"
        
        try:
            headers = {'User-Agent': 'SUMO-Network-Creator/1.0'}
            response = requests.get(url, headers=headers, timeout=300)
            
            if response.status_code == 200:
                with open(output_file, 'wb') as f:
                    f.write(response.content)
                print(f"✅ OSM 数据已保存到 {output_file}")
                return True
            else:
                print(f"❌ 下载失败：HTTP {response.status_code}")
                return False
                
        except Exception as e:
            print(f"❌ 下载异常：{e}")
            return False
    
    def osm_to_net(self, osm_file: str, net_file: str, simplify: bool = True) -> bool:
        """将 OSM 文件转换为 SUMO 路网"""
        
        cmd = ["netconvert", 
               "--osm-files", osm_file,
               "--output-file", net_file]
        
        if simplify:
            cmd.extend([
                "--geometry.remove",
                "--roundabouts.guess",
                "--ramps.guess",
                "--junctions.join",
                "--tls.guess-signals",
                "--tls.discard-simple",
                "--remove-edges.isolated",
            ])
        
        try:
            result = subprocess.run(cmd, capture_output=True, text=True)
            if result.returncode == 0:
                print(f"✅ 路网已生成：{net_file}")
                return True
            else:
                print(f"❌ 转换失败：{result.stderr}")
                return False
        except Exception as e:
            print(f"❌ 转换异常：{e}")
            return False
    
    def generate_random_trips(self, net_file: str, trips_file: str, 
                             end_time: int = 3600, period: float = 1.0) -> bool:
        """为真实路网生成随机交通需求"""
        
        random_trips_script = os.path.join(self.sumo_tools, "randomTrips.py")
        
        cmd = [
            "python3", random_trips_script,
            "-n", net_file,
            "-o", trips_file,
            "-e", str(end_time),
            "-p", str(period),
            "--random"
        ]
        
        try:
            subprocess.run(cmd, check=True)
            print(f"✅ 交通需求已生成：{trips_file}")
            return True
        except subprocess.CalledProcessError as e:
            print(f"❌ 生成需求失败：{e}")
            return False
    
    def create_sumo_config(self, net_file: str, route_file: str, 
                          config_file: str, begin: int = 0, end: int = 3600):
        """创建 SUMO 配置文件"""
        
        config_xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <input>
        <net-file value="{net_file}"/>
        <route-files value="{route_file}"/>
    </input>
    <time>
        <begin value="{begin}"/>
        <end value="{end}"/>
    </time>
    <processing>
        <ignore-route-errors value="true"/>
    </processing>
    <output>
        <tripinfo-output value="tripinfo.xml"/>
        <summary-output value="summary.xml"/>
    </output>
</configuration>"""
        
        with open(config_file, 'w') as f:
            f.write(config_xml)
        print(f"✅ 配置文件已创建：{config_file}")

# 使用示例
if __name__ == "__main__":
    creator = RealNetworkCreator()
    
    # 设置目标区域（北京 CBD）
    bbox = (116.447, 39.908, 116.467, 39.918)
    
    # 1. 下载 OSM 数据
    if creator.download_osm_data(bbox, "beijing_cbd.osm"):
        # 2. 转换为 SUMO 路网
        if creator.osm_to_net("beijing_cbd.osm", "beijing_cbd.net.xml"):
            # 3. 生成交通需求
            if creator.generate_random_trips("beijing_cbd.net.xml", 
                                            "beijing_trips.xml", 
                                            end_time=1800, period=0.5):
                # 4. 创建配置文件
                creator.create_sumo_config("beijing_cbd.net.xml",
                                          "beijing_trips.xml",
                                          "beijing.sumocfg")
                print("\n🎉 完整 SUMO 仿真环境已创建！")
                print("运行命令：sumo-gui -c beijing.sumocfg")
```

运行脚本：

```bash
# 安装依赖
pip3 install requests

# 运行
python3 create_real_network.py
```

### 4.5 其他格式转换

```bash
# 从 OpenDRIVE 转换
netconvert --opendrive road.xodr -o road.net.xml

# 从 Shapefile 转换
netconvert --shapefile-prefix roads \
  --shapefile.guess-projection \
  --output-file roads.net.xml

# 从 MATsim 转换
netconvert --matsim-network network.xml -o sumo.net.xml
```

---

## 五、大规模车辆生成

### 5.1 使用 randomTrips.py

#### 基本生成

```bash
# 生成 1000 辆车（1小时）
python3 $SUMO_HOME/tools/randomTrips.py \
  -n network.net.xml \
  -o trips.xml \
  -e 3600 \
  -p 3.6

# 生成 10000 辆车（1小时）
python3 $SUMO_HOME/tools/randomTrips.py \
  -n network.net.xml \
  -o trips.xml \
  -e 3600 \
  -p 0.36
```

#### 高级参数

```bash
# 指定时间范围和密度
python3 $SUMO_HOME/tools/randomTrips.py \
  -n network.net.xml \
  -o trips.xml \
  -b 0 \              # 开始时间
  -e 7200 \           # 结束时间（2小时）
  -p 0.1 \            # 每 0.1 秒一辆车
  --random \          # 随机种子
  --fringe-factor 10  # 边界车辆生成因子
  --min-distance 100  # 最小行程距离
  --max-distance 5000 # 最大行程距离
```

#### 分时段生成（模拟高峰期）

```bash
# 创建时间段配置文件
cat > periods.txt << EOF
0.0-3600:0.5    # 0-1小时：每0.5秒一辆
3600-7200:0.1   # 1-2小时：每0.1秒一辆（高峰期）
7200-10800:0.3  # 2-3小时：每0.3秒一辆
EOF

# 使用时间段生成
python3 $SUMO_HOME/tools/randomTrips.py \
  -n network.net.xml \
  -o trips.xml \
  --period-file periods.txt
```

### 5.2 使用流量定义（flowrouter.py）

创建 `flow_definitions.xml`：

```xml
<flows>
    <flow id="flow1" from="edge1" to="edge2" number="2000" begin="0" end="3600"/>
    <flow id="flow2" from="edge3" to="edge4" number="5000" begin="0" end="3600"/>
    <flow id="flow3" from="edge5" to="edge6" number="3000" begin="1800" end="7200"/>
</flows>
```

生成路由：

```bash
python3 $SUMO_HOME/tools/flowrouter.py \
  -n network.net.xml \
  -f flow_definitions.xml \
  -o routes.xml
```

### 5.3 使用 OD 矩阵

创建 `od_matrix.xml`：

```xml
<od-matrix>
    <cell from="zone1" to="zone2" amount="5000"/>
    <cell from="zone1" to="zone3" amount="3000"/>
    <cell from="zone2" to="zone1" amount="4000"/>
</od-matrix>
```

生成需求：

```bash
python3 $SUMO_HOME/tools/od2trips.py \
  -n network.net.xml \
  -d od_matrix.xml \
  -o trips.xml
```

### 5.4 Python 脚本生成（灵活控制）

```python
import random
import xml.etree.ElementTree as ET
from typing import List, Tuple

class LargeScaleVehicleGenerator:
    """大规模车辆需求生成器"""
    
    def __init__(self, net_file: str):
        self.net_file = net_file
        self.edges = self._get_edges()
        
    def _get_edges(self) -> List[str]:
        """从路网中提取所有边"""
        tree = ET.parse(self.net_file)
        root = tree.getroot()
        edges = []
        for edge in root.findall('edge'):
            edge_id = edge.get('id')
            if edge_id and not edge_id.startswith(':'):
                edges.append(edge_id)
        return edges
    
    def generate_vehicles(self, 
                         num_vehicles: int,
                         output_file: str,
                         time_range: Tuple[float, float] = (0, 3600)):
        """生成指定数量的车辆"""
        
        root = ET.Element('routes')
        
        # 车辆类型
        vtype = ET.SubElement(root, 'vType')
        vtype.set('id', 'car')
        vtype.set('accel', '2.6')
        vtype.set('decel', '4.5')
        vtype.set('length', '5.0')
        vtype.set('maxSpeed', '20.0')
        
        # 生成车辆
        begin, end = time_range
        for i in range(num_vehicles):
            vehicle = ET.SubElement(root, 'vehicle')
            vehicle.set('id', f'veh_{i}')
            vehicle.set('type', 'car')
            
            # 随机出发时间
            depart_time = random.uniform(begin, end)
            vehicle.set('depart', f'{depart_time:.2f}')
            
            # 随机路线
            from_edge = random.choice(self.edges)
            to_edge = random.choice(self.edges)
            route = ET.SubElement(vehicle, 'route')
            route.set('edges', f'{from_edge} {to_edge}')
        
        # 保存
        tree = ET.ElementTree(root)
        tree.write(output_file, encoding='utf-8', xml_declaration=True)
        print(f'✅ 已生成 {num_vehicles} 辆车')

# 使用
generator = LargeScaleVehicleGenerator('city.net.xml')
generator.generate_vehicles(10000, 'vehicles.rou.xml')
```

### 5.5 性能优化

```bash
# 使用 SUMO 的 scale 参数放大需求
sumo -c config.sumocfg --scale 10  # 需求放大 10 倍

# 使用二进制格式
python3 $SUMO_HOME/tools/xml2protobuf.py trips.xml trips.pb
```

---

## 六、与 Spring Boot 集成

### 6.1 方案一：命令行调用（简单）

```java
@Service
public class SumoSimulationService {

    @Value("${sumo.bin.path}")
    private String sumoBinPath;

    @Value("${sumo.config.dir}")
    private String sumoConfigDir;

    public SimulationResult runSimulation(Long taskId, String configFileName) {
        ProcessBuilder pb = new ProcessBuilder(
            sumoBinPath,
            "-c", sumoConfigDir + "/" + configFileName,
            "--no-warnings",
            "--quit-on-end"
        );
        pb.redirectErrorStream(true);
        
        try {
            Process process = pb.start();
            String logs = readStream(process.getInputStream());
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return parseAndSaveResult(taskId);
            } else {
                throw new RuntimeException("SUMO 仿真失败: " + logs);
            }
        } catch (Exception e) {
            throw new RuntimeException("SUMO 执行异常", e);
        }
    }
}
```

### 6.2 方案二：TraCI 实时交互

```java
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Simulation;

@Service
public class SumoTraCIClient {

    public void runInteractiveSimulation() {
        SumoTraciConnection conn = new SumoTraciConnection("sumo", "simulation.sumocfg");
        
        try {
            conn.runServer();
            
            for (int step = 0; step < 3600; step++) {
                conn.do_timestep();
                
                int vehicleCount = (int) conn.do_job_get(Vehicle.getIDCount());
                // 处理实时数据...
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 6.3 数据库设计

```sql
CREATE TABLE simulation_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(200),
    config_file VARCHAR(500),
    status VARCHAR(50),
    submit_time DATETIME,
    finish_time DATETIME
);

CREATE TABLE simulation_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT,
    metric_name VARCHAR(100),
    metric_value DOUBLE,
    timestamp DATETIME,
    FOREIGN KEY (task_id) REFERENCES simulation_task(id)
);
```

---

## 七、常见问题与解决方案

### 7.1 安装问题

**问题：找不到 sumo 命令**
```bash
# 解决方案
which sumo
export PATH="/opt/homebrew/bin:$PATH"
source ~/.zshrc
```

**问题：netconvert 不可用**
```bash
# 重新安装
brew uninstall sumo
brew install sumo
# 或从源码编译
brew install --build-from-source sumo
```

### 7.2 路网问题

**问题：路网太大**
```bash
# 只保留主要道路
netconvert --osm-files city.osm \
  --keep-edges.by-type highway.motorway,highway.trunk,highway.primary \
  -o main_roads.net.xml
```

**问题：车辆无法通过路口**
```bash
# 添加内部车道
netconvert --osm-files city.osm \
  --junctions.corner-detail 5 \
  --junctions.internal-detail 10 \
  -o city.net.xml
```

### 7.3 性能问题

**问题：仿真速度慢**
```bash
# 使用命令行模式（无 GUI）
sumo -c config.sumocfg

# 减少输出
sumo -c config.sumocfg --no-warnings --no-step-log

# 使用多线程
sumo -c config.sumocfg --threads 4
```

**问题：内存不足**
```bash
# 分批生成车辆
for i in {1..10}; do
  python3 $SUMO_HOME/tools/randomTrips.py \
    -n network.net.xml \
    -o trips_$i.xml \
    --seed $i
done
```

### 7.4 数据问题

**问题：OSM 下载失败**
```python
# 使用备用 API
url = "http://overpass-api.de/api/map?bbox={},{},{},{}"
# 或减小区域
# 或使用 requests 库并设置超时
```

**问题：转换后路网不完整**
```bash
# 检查原始数据
netconvert --osm-files city.osm -o city.net.xml --verbose
# 查看警告信息
# 尝试不同的参数组合
```

---

## 附录

### A. 常用命令速查

```bash
# 仿真
sumo -c config.sumocfg                    # 命令行仿真
sumo-gui -c config.sumocfg                # GUI 仿真

# 路网
netconvert --osm-files input.osm -o output.net.xml  # OSM 转换
netedit network.net.xml                   # 编辑路网
netgenerate --grid --grid.number=5 -o grid.net.xml  # 生成网格

# 需求
python3 $SUMO_HOME/tools/randomTrips.py -n net.xml -o trips.xml
duarouter -n net.xml -t trips.xml -o routes.xml

# 输出
sumo -c config.sumocfg --tripinfo-output tripinfo.xml
sumo -c config.sumocfg --summary-output summary.xml
```

### B. 文件格式说明

| 文件类型 | 扩展名 | 用途 |
|---------|--------|------|
| 路网文件 | .net.xml | 定义道路网络 |
| 需求文件 | .rou.xml | 定义车辆和路线 |
| 配置文件 | .sumocfg | 仿真配置 |
| OSM 文件 | .osm | OpenStreetMap 数据 |
| 输出文件 | .xml | 仿真结果 |

### C. 有用的资源

- 官方文档：https://sumo.dlr.de/docs/
- 教程：https://sumo.dlr.de/docs/Tutorials/
- 论坛：https://sourceforge.net/p/sumo/mailman/
- GitHub：https://github.com/eclipse/sumo

---

**祝您使用 SUMO 愉快！如有问题，请参考官方文档或社区支持。**