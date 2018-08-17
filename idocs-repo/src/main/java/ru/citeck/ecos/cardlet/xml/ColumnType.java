//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.08.14 at 11:20:52 AM GMT+07:00 
//


package ru.citeck.ecos.cardlet.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for columnType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="columnType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="left"/>
 *     &lt;enumeration value="right"/>
 *     &lt;enumeration value="top"/>
 *     &lt;enumeration value="bottom"/>
 *     &lt;enumeration value="disabled"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "columnType")
@XmlEnum
public enum ColumnType {

    @XmlEnumValue("left")
    LEFT("left"),
    @XmlEnumValue("right")
    RIGHT("right"),
    @XmlEnumValue("top")
    TOP("top"),
    @XmlEnumValue("bottom")
    BOTTOM("bottom"),
    @XmlEnumValue("disabled")
    DISABLED("disabled");
    private final String value;

    ColumnType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ColumnType fromValue(String v) {
        for (ColumnType c: ColumnType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}