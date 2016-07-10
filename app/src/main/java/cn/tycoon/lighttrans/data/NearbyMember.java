package cn.tycoon.lighttrans.data;


/**
 * @author sidwang
 */
public class NearbyMember {
    public static final int SEX_MALE = 0;
    public static final int SEX_FEMALE = 1;
    public static final int SEX_ALL = 2;

    public String nickName;
    public int age;
    public int sex = -1;
    public double latitude;
    public double longitude;
    public long uin;
    public double distance;
    
    //ver2.0
    public double degree;
    public double radians;
}
