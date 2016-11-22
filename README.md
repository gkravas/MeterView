 [ ![Download](https://api.bintray.com/packages/gkravas/android/MeterView/images/download.svg) ](https://bintray.com/gkravas/android/MeterView/_latestVersion)

# MeterView
MeterView is a simple android meter component. It can handle exceeding values also by redrawing the whole scale.

![Preview](/screenshots/preview.gif)

### Supported Attributes ###
| Attribute      	      | Format   | Default | Description     |
| :---           	      | :---:    | :---:   | :---            |
| `meter_min_value`     | float    | 0       | Scales meter min value |
| `meter_max_value`     | float    | 20      | Scales meter min value |
| `meter_logo`          | drawable | null    | Scales meter min value |
| `meter_faceColor`     | color    | FFFFFF  | Meter's background color |
| `meter_scaleColor`    | color    | 616161  | Meter's scale color |
| `meter_rimColor`      | color    | BDBDBD  | Meter's rim color |
| `meter_valueColor`    | color    | 000000  | Meter's value color, found at the bottom |

Download
------
####Gradle:

Artifact can be found via jCenter

```groovy
compile 'com.gkravas.meterview:meterview:1.0.0@aar'
```
