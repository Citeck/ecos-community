
package ru.citeck.ecos.journals.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for groupAction complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="groupAction">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="param" type="{http://www.citeck.ru/ecos/journals/1.0}option" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="evaluator" type="{http://www.citeck.ru/ecos/journals/1.0}evaluator" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="title" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" type="{http://www.citeck.ru/ecos/journals/1.0}groupActionType" default="selected" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "groupAction", propOrder = {
    "param",
    "evaluator"
})
public class GroupAction {

    protected List<Option> param;
    protected Evaluator evaluator;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "title", required = true)
    protected String title;
    @XmlAttribute(name = "type")
    protected GroupActionType type;

    /**
     * Gets the value of the param property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the param property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParam().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Option }
     * 
     * 
     */
    public List<Option> getParam() {
        if (param == null) {
            param = new ArrayList<Option>();
        }
        return this.param;
    }

    /**
     * Gets the value of the evaluator property.
     * 
     * @return
     *     possible object is
     *     {@link Evaluator }
     *     
     */
    public Evaluator getEvaluator() {
        return evaluator;
    }

    /**
     * Sets the value of the evaluator property.
     * 
     * @param value
     *     allowed object is
     *     {@link Evaluator }
     *     
     */
    public void setEvaluator(Evaluator value) {
        this.evaluator = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link GroupActionType }
     *     
     */
    public GroupActionType getType() {
        if (type == null) {
            return GroupActionType.SELECTED;
        } else {
            return type;
        }
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link GroupActionType }
     *     
     */
    public void setType(GroupActionType value) {
        this.type = value;
    }

}
