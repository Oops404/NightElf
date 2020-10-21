# NightElf
### express hacker
> android studio 工程项目，编译可得Android平台APK，记录手机三轴陀螺仪、加速度仪的参数，包括：
> > * lAX 线性加速度X轴方向
> > * lAY 线性加速度Y轴方向
> > * lAZ 线性加速度Z轴方向

​    **PS**：线性加速度已剔除三轴方向上的重力加速度分量。

> > * PITCH，ROLL，YAW 陀螺仪姿态
> > * MX，MY，MZ 位移

# expres-anim
> 这里是通过[Blender](https://www.blender.org/)加载传感器数据，套用给OBJ模型，来实现动画回放。


# notice
* 安装后，在应用设置中打开该APP自启动，禁用该APP省电优化。
* 点击后自动后台运行并最小化。
* 每5分钟左右记录一次数据到磁盘，数据将追加到手机根目录/.night/elf/elf.csv 文件中。*注意：*该文件夹可能隐藏，需打开文件资源管理器的显示隐藏文件选项。
