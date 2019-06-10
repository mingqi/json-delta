import java.util.*;

class Delta{
	String deltaType;
	Object deltaValue;

	public Delta(String deltaType){
		this.deltaType = deltaType;
	}
	public Delta(String deltaType, Object deltaValue){
		this.deltaType = deltaType;
		this.deltaValue = deltaValue;
	}

	@Override
	public String toString(){
		String s = "{delta_type="+deltaType;
		if( deltaValue != null){
			s = s + ", delta_value="+deltaValue;
		}
		s = s+"}";
		return s;
	}
}

/**
 * The processing of comparing and restoring JSON is recursive,
 * We need to respectively implement a Diff for each JSON type
**/
interface Diff{

	public Delta diff( Object v2 );
	public Object apply( Delta delta );

	public static Diff diffFactory(Object v1){
		if( v1 instanceof Map){
			return new ObjectDiff(v1);
		}else if( v1 instanceof List){
			return new ListDiff(v1);
		}else{
			return new PrimitiveDiff(v1);
		}
	}
}

/**
 * Privitive JSON type, String, Integer ..etc.
**/
class PrimitiveDiff implements Diff{

	private Object v1;

	public PrimitiveDiff(Object v1){
		this.v1 = v1;
	}

	@Override
	public Delta diff(Object v2){
		if( v2 == null){
			return new Delta("REMOVE");
		}
		if( v1 == null ){
			return new Delta("NEW", v2);		
		}

		if(v1.getClass() != v2.getClass() || !v1.equals(v2) ){
			// It's always is REPLACE if two object with difference Object type
			return new Delta("REPLACE", v2);
		}

		return null;
	}

	@Override
	public Object apply( Delta delta ){
		if( delta.deltaType.equals("REMOVE")){
			return null;
		}else{
			return delta.deltaValue;
		}
	}
}


/**
 * List JSON type
 **/
class ListDiff implements Diff{
	private List v1;

	public ListDiff(Object v1Obj){
		v1 = (List) v1Obj;
	}

	@Override
	public Delta diff(Object v2Obj){
		List deltaValue = new ArrayList(); 
		Delta diff = new Delta("CHANGE", deltaValue);
		
		List v2 = (List) v2Obj;
		int maxSize = Math.max(v1.size(), v2.size());
		for(int index=0; index<maxSize; index++){
			Object v1Element = index >= v1.size() ? null : v1.get(index);
			Object v2Element = index >= v2.size() ? null : v2.get(index);
			Delta elementDelta = Diff.diffFactory(v1Element).diff(v2Element);
			if(elementDelta != null){
				Map<String, Object> changeElement = new HashMap<String, Object>();
				if( v1Element == null || v2Element == null){
					changeElement.put("index", -1);
				}else{
					changeElement.put("index", index);
				}
				changeElement.put("delta", elementDelta);
				deltaValue.add(changeElement);
			}
		}
		return diff;
	}

	@Override
	public Object apply( Delta delta ){
		List<Map<String, Object>> changes = (List<Map<String, Object>>) delta.deltaValue;
		for(Map<String, Object> change : changes){
			int index = (int) change.get("index");
			Delta indexDelta = (Delta) change.get("delta");

			Object v1Value = index > 0 ? v1.get(index): null;

			if(indexDelta.deltaType.equals("NEW")){
				// always append element at the end
				v1.add(indexDelta.deltaValue);
			}else if(indexDelta.deltaType.equals("REMOVE") ){
				// always remove element at the end
				v1.remove(v1.size()-1);
			}else{
				Object newObj = Diff.diffFactory(v1Value).apply(indexDelta);
				v1.set(index, newObj);
			}
		}
		return v1;
	}
}

/**
 * Object JSON type
 **/
class ObjectDiff implements Diff{

	private Map<String, Object> v1;

	public ObjectDiff(Object v1Obj){
		if( v1Obj != null ){
			this.v1 = (Map<String, Object>) v1Obj;
		}
	}

	@Override
	public Delta diff( Object v2Obj){

		if( v2Obj == null){
			return new Delta("REMOVE");
		}

		if(v1 == null){
			return new Delta("NEW", v2Obj);
		}

		if(v1.getClass() != v2Obj.getClass()){
			// It's always is REPLACE if two object with difference Object type
			return new Delta("REPLACE", v2Obj);
		}

		// make some changes on v1
		Map<String, Object> v2 = (Map<String, Object>) v2Obj;

		Map<String, Object> deltaValue = new HashMap<String, Object>();
		Delta delta = new Delta("CHANGE", deltaValue);

		Set<String> allFieldNames = new HashSet<String>();
		allFieldNames.addAll(v1.keySet());
		allFieldNames.addAll(v2.keySet());

		for(String fieldName : allFieldNames){
			Object v1FieldValue = v1.get(fieldName);
			Object v2FieldValue = v2.get(fieldName);
			Delta fieldDelta = Diff.diffFactory(v1FieldValue).diff(v2FieldValue);
			if( fieldDelta != null){
				deltaValue.put(fieldName, fieldDelta);
			}
		}

		if(deltaValue.size() == 0) return null;
		return delta;
	}

	@Override
	public Object apply(Delta delta){
		if( delta.deltaType.equals("REPLACE")){
			return delta.deltaValue;
		}else if(delta.deltaType.equals("REMOVE")){
			return null;
		}else if(delta.deltaType.equals("CHANGE")){
			// recusively deal with all sub delta
			Map<String, Delta> deltaValue = (Map<String, Delta>) delta.deltaValue;
			for(Map.Entry<String, Delta> fieldChange: deltaValue.entrySet()){
				String fieldName = fieldChange.getKey();
				Delta fieldDelta = fieldChange.getValue();

				Object v1Value = v1.get(fieldName);
				Object newObj = Diff.diffFactory(v1Value).apply(fieldDelta);
				if(newObj == null){
					v1.remove(fieldName);
				}else{
					v1.put(fieldName, newObj);
				}
			}
			return v1;
		}

		return null;
	}
}


public class JsonDelta{

	public static void main(String[] args) {
		Map<String, Object> v1 = new HashMap<String, Object>();
		v1.put("firstName", "John");
		v1.put("lastName", "Smith");
    	v1.put("age", 27);
    	v1.put("isAlive", true);
			Map<String, String> address = new HashMap<String, String>();
			address.put("streetAddress", "21 2nd Street");
			address.put("city", "New York");
			address.put("state", "NY");
			address.put("postalCode", "10021-3100");
		v1.put("address", address);
			List<Map<String, String>> phoneNumbers = new ArrayList<Map<String, String>>();
			Map<String, String> phone = new HashMap<String, String>();
			phone.put("type", "home");
			phone.put("number", "212 555-1234");
			phoneNumbers.add(phone);
			phone = new HashMap<String, String>();
			phone.put("type", "office");
			phone.put("number", "646 555-4567");
			phoneNumbers.add(phone);
			phone = new HashMap<String, String>();
			phone.put("type", "mobile");
			phone.put("number", "123 456-7890");
			phoneNumbers.add(phone);
		v1.put("phoneNumbers", phoneNumbers);

		Map<String, Object> v2 = new HashMap<String, Object>();
		v2.put("firstName", "John");
		v2.put("lastName", "Smith");
    	v2.put("age", 30);
			address = new HashMap<String, String>();
			address.put("streetAddress", "21 2nd Street");
			address.put("city", "New York");
			address.put("state", "NY");
			address.put("postalCode", "20021-3100");
		v2.put("address", address);
			phoneNumbers = new ArrayList<Map<String, String>>();
			phone = new HashMap<String, String>();
			phone.put("type", "home");
			phone.put("number", "212 555-1234");
			phoneNumbers.add(phone);
			phone = new HashMap<String, String>();
			phone.put("type", "office");
			phone.put("number", "646 555-4567");
			phoneNumbers.add(phone);
		v2.put("phoneNumbers", phoneNumbers);

		Diff diff = Diff.diffFactory(v1);
		Delta delta = diff.diff(v2);
		
		System.out.println("Delta Data is:");
		System.out.println(delta);

		System.out.println("Recovered data from Delta:");
		System.out.println(diff.apply(delta));
	}
}
