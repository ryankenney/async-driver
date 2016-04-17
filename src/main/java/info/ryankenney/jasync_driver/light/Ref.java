package info.ryankenney.jasync_driver.light;

/**
 * A basic reference to an object. Used in cases where we need a final
 * reference, but a dynamic value.
 */
public class Ref<T> {

    private T obj;

    public Ref() {
        this(null);
    }
    
    public Ref(T obj) {
        this.obj = obj;
    }
    
    public T get() {
        return this.obj;
    }
    
    public Ref<T> set(T obj) {
        this.obj = obj;
        return this;
    }
}
