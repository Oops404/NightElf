# NightElf
### express hacker
> android studio 工程项目，编译可得Android平台APK，记录手机三轴陀螺仪、加速度仪的参数，包括：
> > * AX X轴方向
> > * AY Y轴方向
> > * AZ Z轴方向
> > * lAX 重力线性加速度X轴方向
> > * lAY 重力线性加速度Y轴方向
> > * lAZ 重力线性加速度Z轴方向

> > * PITCH 围绕X轴旋转，俯仰角
> > * ROLL 围绕Y轴旋转，偏航角
> > * YAW 围绕Z轴旋转，翻滚角

> > * IN_PITCH 机内，俯仰角
> > * IN_ROLL 机内，偏航角
> > * IN_AZIMUTH 机内，方位角

# expres-anim
> 这里是通过[Blender](https://www.blender.org/)加载姿态数据，套用给OBJ模型，来实现动画回放。


# notice
* 安装后，在应用设置中打开该APP自启动，禁用该APP省电优化。
* 点击后自动后台运行并最小化。
* 每5分钟左右记录一次数据到磁盘，数据将追加到手机根目录/.night/elf/elf.csv 文件中。**注意：**该文件夹可能隐藏，需打开文件资源管理器的显示隐藏文件选项。
