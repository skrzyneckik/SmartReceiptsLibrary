package co.smartreceipts.android.model.impl;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.math.BigDecimal;

import co.smartreceipts.android.model.Price;
import co.smartreceipts.android.model.WBCurrency;
import co.smartreceipts.android.model.utils.ModelUtils;

/**
 * Defines an immutable implementation of the {@link co.smartreceipts.android.model.Price} interface
 *
 * @author williambaumann
 */
public final class ImmutablePriceImpl implements Price, android.os.Parcelable {

    private final BigDecimal mPrice;
    private final WBCurrency mCurrency;

    public ImmutablePriceImpl(@NonNull BigDecimal price, @NonNull WBCurrency currency) {
        mPrice = price;
        mCurrency = currency;
    }

    private ImmutablePriceImpl(Parcel in) {
        this.mPrice = new BigDecimal(in.readFloat());
        this.mCurrency = WBCurrency.getInstance(in.readString());
    }

    @Override
    public float getPriceAsFloat() {
        return mPrice.floatValue();
    }

    @Override
    @NonNull
    public BigDecimal getPrice() {
        return mPrice;
    }

    @Override
    @NonNull
    public String getDecimalFormattedPrice() {
        return ModelUtils.getDecimalFormattedValue(mPrice);
    }

    @Override
    @NonNull
    public String getCurrencyFormattedPrice() {
        return ModelUtils.getCurrencyFormattedValue(mPrice, mCurrency);
    }

    @Override
    @NonNull
    public WBCurrency getCurrency() {
        return mCurrency;
    }

    @Override
    @NonNull
    public String getCurrencyCode() {
        return mCurrency.getCurrencyCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImmutablePriceImpl that = (ImmutablePriceImpl) o;

        if (!mCurrency.equals(that.mCurrency)) return false;
        if (!mPrice.equals(that.mPrice)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mPrice.hashCode();
        result = 31 * result + mCurrency.hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(getPriceAsFloat());
        dest.writeString(getCurrencyCode());
    }

    public static final Creator<ImmutablePriceImpl> CREATOR = new Creator<ImmutablePriceImpl>() {
        public ImmutablePriceImpl createFromParcel(Parcel source) {
            return new ImmutablePriceImpl(source);
        }

        public ImmutablePriceImpl[] newArray(int size) {
            return new ImmutablePriceImpl[size];
        }
    };

}
