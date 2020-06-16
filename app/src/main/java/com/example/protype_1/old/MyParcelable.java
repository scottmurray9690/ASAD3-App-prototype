package com.example.protype_1.old;

import android.os.Parcel;
import android.os.Parcelable;

public class MyParcelable implements Parcelable {
    // You can include parcel data types
    private int mData;
    private String mName;
    private spectogram spec;


    // We can also include child Parcelable objects. Assume MySubParcel is such a Parcelable:
    //private MySubParcelable mInfo;

    // This is where you write the values you want to save to the `Parcel`.
    // The `Parcel` class has methods defined to help you save all of your values.
    // Note that there are only methods defined for simple values, lists, and other Parcelable objects.
    // You may need to make several classes Parcelable to send the data you want.
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mData);
        out.writeString(mName);

        //out.writeParcelable(mInfo, flags);
    }

    // Using the `in` variable, we can retrieve the values that
    // we originally wrote into the `Parcel`.  This constructor is usually
    // private so that only the `CREATOR` field can access.
    private MyParcelable(Parcel in) {
        mData = in.readInt();
        mName = in.readString();
        //mInfo = in.readParcelable(MySubParcelable.class.getClassLoader());
    }

    public MyParcelable() {
        // Normal actions performed by class, since this is still a normal object!
    }

    // In the vast majority of cases you can simply return 0 for this.
    // There are cases where you need to use the constant `CONTENTS_FILE_DESCRIPTOR`
    // But this is out of scope of this tutorial
    @Override
    public int describeContents() {
        return 0;
    }

    // After implementing the `Parcelable` interface, we need to create the
    // `Parcelable.Creator<MyParcelable> CREATOR` constant for our class;
    // Notice how it has our class specified as its type.
    public static final Parcelable.Creator<MyParcelable> CREATOR
            = new Parcelable.Creator<MyParcelable>() {

        // This simply calls our new constructor (typically private) and
        // passes along the unmarshalled `Parcel`, and then returns the new object!
        @Override
        public MyParcelable createFromParcel(Parcel in) {
            return new MyParcelable(in);
        }

        // We just need to copy this and change the type to match our class.
        @Override
        public MyParcelable[] newArray(int size) {
            return new MyParcelable[size];
        }
    };
}