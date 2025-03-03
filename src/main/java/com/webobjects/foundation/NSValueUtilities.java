package com.webobjects.foundation;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class NSValueUtilities {
	public static <T> NSArray<T> arrayValue(final Object obj) {
		return arrayValueWithDefault(obj, null);
	}

	public static <T> NSArray<T> arrayValueWithDefault(final Object obj, final NSArray<T> def) {
		NSArray<T> value = def;
		if (obj != null) {
			if (obj instanceof NSArray) {
				value = (NSArray<T>) obj;
			} else if (obj instanceof String) {
				String strValue = ((String) obj).trim();
				if (strValue.length() > 0) {
					if (strValue.charAt(0) != '(') {
						strValue = "(" + strValue + ")";
					}
					value = (NSArray<T>) NSPropertyListSerialization.propertyListFromString(strValue);
					if (value == null) {
						throw new IllegalArgumentException("Failed to parse an array from the value '" + obj + "'.");
					}
				}
			} else {
				throw new IllegalArgumentException("Failed to parse an array from the value '" + obj + "'.");
			}
		}
		return value;
	}

	public static BigDecimal bigDecimalValue(final Object obj) {
		return bigDecimalValueWithDefault(obj, null);
	}

	public static BigDecimal bigDecimalValueWithDefault(final Object obj, final BigDecimal def) {
		BigDecimal value = def;
		if (obj != null) {
			if (obj instanceof BigDecimal) {
				value = (BigDecimal) obj;
			} else if (obj instanceof String) {
				final String strValue = ((String) obj).trim();
				if (strValue.length() > 0) {
					value = new BigDecimal(strValue);
				}
			} else if (obj instanceof Integer) {
				value = new BigDecimal(((Integer) obj).intValue());
			} else if (obj instanceof Long) {
				value = new BigDecimal(((Long) obj).longValue());
			} else if (obj instanceof Float) {
				value = new BigDecimal(((Float) obj).floatValue());
			} else if (obj instanceof Double) {
				value = new BigDecimal(((Double) obj).doubleValue());
			} else if (obj instanceof Number) {
				value = new BigDecimal(((Number) obj).doubleValue());
			} else if (obj instanceof Boolean) {
				value = new BigDecimal(((Boolean) obj).booleanValue() ? 1 : 0);
			} else {
				throw new IllegalArgumentException("Failed to parse a BigDecimal from the value '" + obj + "'.");
			}
		}
		return value;
	}

	public static boolean booleanValue(final Object obj) {
		return booleanValueWithDefault(obj, false);
	}

	public static boolean booleanValueWithDefault(final Object obj, final boolean def) {
		return (obj == null) ? def : BooleanValueWithDefault(obj, def).booleanValue();
	}

	public static Boolean BooleanValueWithDefault(final Object obj, final Boolean def) {
		Boolean flag = def;
		if (obj != null) {
			if (obj instanceof Number) {
				if (((Number) obj).intValue() == 0) {
					flag = Boolean.FALSE;
				} else {
					flag = Boolean.TRUE;
				}
			} else if (obj instanceof String) {
				final String strValue = ((String) obj).trim();
				if (strValue.length() > 0) {
					if ("no".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue)
							|| "n".equalsIgnoreCase(strValue)) {
						flag = Boolean.FALSE;
					} else if ("yes".equalsIgnoreCase(strValue) || "true".equalsIgnoreCase(strValue)
							|| "y".equalsIgnoreCase(strValue)) {
						flag = Boolean.TRUE;
					} else {
						try {
							if (Integer.parseInt(strValue) == 0) {
								flag = Boolean.FALSE;
							} else {
								flag = Boolean.TRUE;
							}
						} catch (final NumberFormatException numberformatexception) {
							throw new IllegalArgumentException(
									"Failed to parse a boolean from the value '" + strValue + "'.");
						}
					}
				}
			} else if (obj instanceof Boolean) {
				flag = (Boolean) obj;
			} else {
				throw new IllegalArgumentException("Failed to parse a boolean from the value '" + obj + "'.");
			}
		}
		return flag;
	}

	@SafeVarargs
	public static <R> Optional<R> coalesce(final Supplier<R>... suppliers) {
		R result = null;
		for (final Supplier<R> sup : suppliers) {
			result = sup.get();
			if (result != null) {
				break;
			}
		}
		return Optional.ofNullable(result);
	}

	@SafeVarargs
	public static <T, R> Optional<R> coalesce(final T val, final Function<T, R>... functions) {
		R result = null;
		if (val != null) {
			for (final Function<T, R> fun : functions) {
				result = fun.apply(val);
				if (result != null) {
					break;
				}
			}
		}
		return Optional.ofNullable(result);
	}

	public static int compare(final int int1, final int int2) {
		return (int1 > int2) ? 1 : ((int1 < int2) ? -1 : 0);
	}

	public static NSData dataValue(final Object obj) {
		return dataValueWithDefault(obj, null);
	}

	public static NSData dataValueWithDefault(final Object obj, final NSData def) {
		NSData value = def;
		if (obj != null) {
			if (obj instanceof NSData) {
				value = (NSData) obj;
			} else if (obj instanceof byte[]) {
				final byte[] byteValue = (byte[]) obj;
				value = new NSData(byteValue, new NSRange(0, byteValue.length), true);
			} else if (obj instanceof String) {
				final String strValue = ((String) obj).trim();
				if (strValue.length() > 0) {
					final Object objValue = NSPropertyListSerialization.propertyListFromString(strValue);
					if (objValue == null || !(objValue instanceof NSData)) {
						throw new IllegalArgumentException("Failed to parse data from the value '" + obj + "'.");
					}
					value = (NSData) objValue;
				}
			} else {
				throw new IllegalArgumentException("Failed to parse data from the value '" + obj + "'.");
			}
		}
		return value;
	}

	public static <K, V> NSDictionary<K, V> dictionaryValue(final Object obj) {
		return dictionaryValueWithDefault(obj, null);
	}

	public static <K, V> NSDictionary<K, V> dictionaryValueWithDefault(final Object obj, final NSDictionary<K, V> def) {
		NSDictionary<K, V> value = def;
		if (obj != null) {
			if (obj instanceof NSDictionary) {
				value = (NSDictionary) obj;
			} else if (obj instanceof String) {
				final String strValue = ((String) obj).trim();
				if (strValue.length() > 0) {
					final Object objValue = NSPropertyListSerialization.propertyListFromString((String) obj);
					if (objValue == null || !(objValue instanceof NSDictionary)) {
						throw new IllegalArgumentException(
								"Failed to parse a dictionary from the value '" + obj + "'.");
					}
					value = (NSDictionary) objValue;
				}
			} else {
				throw new IllegalArgumentException("Failed to parse a dictionary from the value '" + obj + "'.");
			}
		}
		return value;
	}

	public static double doubleValue(final Object obj) {
		return doubleValueWithDefault(obj, 0.0D);
	}

	public static double doubleValueWithDefault(final Object obj, final double def) {
		return (obj == null) ? def : DoubleValueWithDefault(obj, def).doubleValue();
	}

	public static Double DoubleValueWithDefault(final Object obj, final Double def) {
		Double value = def;
		if (obj != null) {
			if (obj instanceof Double) {
				value = (Double) obj;
			} else if (obj instanceof Number) {
				value = ((Number) obj).doubleValue();
			} else if (obj instanceof String) {
				try {
					final String strValue = ((String) obj).trim();
					if (strValue.length() > 0) {
						value = Double.valueOf(strValue);
					}
				} catch (final NumberFormatException numberformatexception) {
					throw new IllegalArgumentException("Failed to parse a double from the value '" + obj + "'.",
							numberformatexception);
				}
			} else if (obj instanceof Boolean) {
				value = ((Boolean) obj).booleanValue() ? 1.0D : def;
			}
		} else {
			value = def;
		}
		return value;
	}

	public static float floatValue(final Object obj) {
		return floatValueWithDefault(obj, 0.0F);
	}

	public static float floatValueWithDefault(final Object obj, final float def) {
		return (obj == null) ? def : FloatValueWithDefault(obj, def).floatValue();
	}

	public static Float FloatValueWithDefault(final Object obj, final Float def) {
		Float value = def;
		if (obj != null) {
			if (obj instanceof Float) {
				value = (Float) obj;
			} else if (obj instanceof Number) {
				value = ((Number) obj).floatValue();
			} else if (obj instanceof String) {
				try {
					final String strValue = ((String) obj).trim();
					if (strValue.length() > 0) {
						value = Float.valueOf(strValue);
					}
				} catch (final NumberFormatException numberformatexception) {
					throw new IllegalArgumentException("Failed to parse a float from the value '" + obj + "'.",
							numberformatexception);
				}
			} else if (obj instanceof Boolean) {
				value = ((Boolean) obj).booleanValue() ? 1.0F : def;
			}
		} else {
			value = def;
		}
		return value;
	}

	public static Integer IntegerValueWithDefault(final Object obj, final Integer def) {
		Integer value = def;
		if (obj != null) {
			if (obj instanceof Integer) {
				value = ((Integer) obj).intValue();
			} else if (obj instanceof Number) {
				value = ((Number) obj).intValue();
			} else if (obj instanceof String) {
				try {
					final String strValue = ((String) obj).trim();
					if (strValue.length() > 0) {
						value = Integer.valueOf(strValue);
					}
				} catch (final NumberFormatException numberformatexception) {
					throw new IllegalArgumentException("Failed to parse an integer from the value '" + obj + "'.",
							numberformatexception);
				}
			} else if (obj instanceof Boolean) {
				value = ((Boolean) obj).booleanValue() ? 1 : def;
			}
		} else {
			value = def;
		}
		return value;
	}

	public static int intValue(final Object obj) {
		return intValueWithDefault(obj, 0);
	}

	public static int intValueWithDefault(final Object obj, final int def) {
		return (obj == null) ? def : IntegerValueWithDefault(obj, def).intValue();
	}

	public static boolean isValidUrl(final String url) {
		return urlValue(url).isPresent();
	}

	public static long longValue(final Object obj) {
		return longValueWithDefault(obj, 0L);
	}

	public static long longValueWithDefault(final Object obj, final long def) {
		return (obj == null) ? def : LongValueWithDefault(obj, def).longValue();
	}

	public static Long LongValueWithDefault(final Object obj, final Long def) {
		Long value = def;
		if (obj != null) {
			if (obj instanceof Long) {
				value = (Long) obj;
			} else if (obj instanceof Number) {
				value = ((Number) obj).longValue();
			} else if (obj instanceof String) {
				try {
					final String strValue = ((String) obj).trim();
					if (strValue.length() > 0) {
						value = Long.valueOf(strValue);
					}
				} catch (final NumberFormatException numberformatexception) {
					throw new IllegalArgumentException("Failed to parse a long from the value '" + obj + "'.",
							numberformatexception);
				}
			} else if (obj instanceof Boolean) {
				value = ((Boolean) obj).booleanValue() ? 1L : def;
			}
		} else {
			value = def;
		}
		return value;
	}

	public static <T> NSMutableArray<T> mutableArrayFromStream(final Stream<T> stream) {
		return stream.collect(NSMutableArray::new, NSMutableArray::add, NSMutableArray::addAll);
	}

	public static <T> NSSet<T> setValue(final Object obj) {
		return setValueWithDefault(obj, null);
	}

	public static <T> NSSet<T> setValueWithDefault(final Object obj, final NSSet<T> def) {
		NSSet<T> value = def;
		if (obj != null) {
			if (obj instanceof NSSet) {
				value = (NSSet) obj;
			} else if (obj instanceof NSArray) {
				value = new NSSet<>((NSArray<T>) obj);
			} else if (obj instanceof String) {
				final NSArray<T> array = arrayValueWithDefault(obj, null);
				if (array != null) {
					value = new NSSet<>(array);
				}
			} else {
				throw new IllegalArgumentException("Failed to parse a set from the value '" + obj + "'.");
			}
		}
		return value;
	}

	public static Optional<URL> urlValue(final String url) {
		try {
			final URL result = new URL(url);
			result.toURI();
			return Optional.of(result);
		} catch (MalformedURLException | URISyntaxException e) {
			return Optional.empty();
		}
	}

	private NSValueUtilities() {
	}
}