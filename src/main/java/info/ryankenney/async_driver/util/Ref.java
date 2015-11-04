package info.ryankenney.async_driver.util;

public class Ref<T> {
	
	T value;
	
	public Ref() {
		this(null);
	}
	
	public Ref(T value) {
		this.value = value;
	}
	
	public T get() {
		return value;
	}
	
	public void set(T value) {
		this.value = value;
	}

}
