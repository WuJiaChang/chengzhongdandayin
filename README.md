# 蓝牙打印APP - 称重单打印系统

## 项目简介

这是一个Android应用程序，用于连接热敏打印机并打印混凝土称重单。通过蓝牙SPP协议与打印机通信，使用ESC/POS标准指令生成打印内容。

## 主要功能

### 1. 蓝牙连接
- 使用Android BluetoothAdapter + BluetoothSocket实现
- 支持SPP协议（串口通信）
- 自动扫描已配对的蓝牙设备
- 实时显示连接状态

### 2. 称重单录入
- 车号输入（支持自动匹配皮重）
- 皮重输入
- 方量输入
- 自动计算净重和毛重

### 3. 自动计算逻辑
- **净重** = 方量 × 随机系数（2.36 - 2.37之间浮动）
- **毛重** = 净重 + 皮重
- 每次打印时随机系数会在2.36-2.37之间变化
- 时间自动获取当前时间

### 4. 车号皮重管理
- 支持手动录入车号和皮重对应关系
- 输入车号自动带出对应的皮重
- 支持添加、删除车号皮重记录
- 数据本地持久化存储（SQLite）

### 5. 打印功能
- 生成ESC/POS标准打印指令
- 支持字体大小、对齐方式、加粗等格式
- 自动切纸
- 打印内容包括：车号、皮重、方量、净重、毛重、时间

## 技术栈

- **开发语言**: Kotlin
- **最低SDK版本**: API 24 (Android 7.0)
- **目标SDK版本**: API 34 (Android 14)
- **构建工具**: Gradle 8.2
- **UI框架**: Material Design 3
- **蓝牙协议**: SPP (Serial Port Profile)
- **打印协议**: ESC/POS

## 项目结构

```
app/
├── src/main/
│   ├── java/com/claw/printerapp/
│   │   ├── adapter/              # RecyclerView适配器
│   │   │   ├── BluetoothDeviceAdapter.kt
│   │   │   └── CarTareAdapter.kt
│   │   ├── bluetooth/            # 蓝牙管理
│   │   │   └── BluetoothManager.kt
│   │   ├── database/             # 数据库管理
│   │   │   └── CarTareDatabase.kt
│   │   ├── model/                # 数据模型
│   │   │   └── CarTareInfo.kt
│   │   ├── print/                # 打印功能
│   │   │   └── EscPosPrinter.kt
│   │   └── MainActivity.kt       # 主界面
│   ├── res/
│   │   ├── layout/               # 布局文件
│   │   │   ├── activity_main.xml
│   │   │   ├── dialog_bluetooth_devices.xml
│   │   │   ├── dialog_manage_car_tare.xml
│   │   │   ├── item_bluetooth_device.xml
│   │   │   └── item_car_tare.xml
│   │   ├── values/               # 资源文件
│   │   └── xml/                  # 配置文件
│   └── AndroidManifest.xml
```

## 安装和使用

### 环境要求

1. Android Studio Arctic Fox (2020.3.1) 或更高版本
2. JDK 17 或更高版本
3. Android SDK API 34
4. 支持蓝牙的Android设备
5. 支持ESC/POS指令的热敏打印机

### 编译步骤

1. 克隆项目到本地
2. 使用Android Studio打开项目
3. 等待Gradle同步完成
4. 连接Android设备或启动模拟器
5. 点击运行按钮或使用命令行：

```bash
# 编译Debug版本
./gradlew assembleDebug

# 编译Release版本
./gradlew assembleRelease
```

### 使用说明

1. **首次使用**：
   - 开启手机蓝牙
   - 将蓝牙打印机与手机配对
   - 打开APP，点击"连接蓝牙打印机"
   - 选择已配对的打印机进行连接

2. **录入称重单**：
   - 输入车号（如果车号已录入，皮重会自动填充）
   - 输入或确认皮重
   - 输入方量
   - 系统自动计算净重和毛重

3. **管理车号皮重**：
   - 点击"车号皮重管理"按钮
   - 输入车号和皮重，点击"添加"
   - 可以删除已有的记录

4. **打印**：
   - 确认信息无误后，点击"打印"按钮
   - 打印机会输出称重单

## 权限说明

应用需要以下权限：

- `BLUETOOTH` - 蓝牙连接
- `BLUETOOTH_ADMIN` - 蓝牙管理
- `BLUETOOTH_CONNECT` - 蓝牙连接（Android 12+）
- `BLUETOOTH_SCAN` - 蓝牙扫描（Android 12+）
- `ACCESS_FINE_LOCATION` - 精确定位（用于蓝牙扫描）

## 注意事项

1. 蓝牙打印机必须使用SPP协议
2. 打印机必须支持ESC/POS指令集
3. 首次使用需要授权所有权限
4. 车号皮重数据存储在本地数据库中

## 打印格式示例

```
        混凝土称重单
--------------------------------

车号：京A12345
皮重：15000 kg
方量：10.5 m³
净重：24885.25 kg
毛重：39885.25 kg
时间：2026-03-18 14:30:00

--------------------------------

        谢谢惠顾！
```

## 故障排除

### 无法连接打印机
- 确认蓝牙已开启
- 确认打印机已与手机配对
- 重启蓝牙和打印机
- 检查应用权限

### 打印失败
- 确认已连接打印机
- 确认打印机支持ESC/POS指令
- 检查打印机纸张是否充足
- 尝试断开重连

### 车号皮重不自动填充
- 确认车号皮重已录入
- 检查车号是否正确
- 清空重新输入

## 开发者信息

- 开发工具: Android Studio
- 开发语言: Kotlin
- 构建工具: Gradle

## 许可证

本项目仅供学习和参考使用。
