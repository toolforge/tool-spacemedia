package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Content_table">Mediawiki
 * Content table</a>
 *
 * <pre>
 * +-----------------+----------------------+------+-----+---------+----------------+
 * | Field           | Type                 | Null | Key | Default | Extra          |
 * +-----------------+----------------------+------+-----+---------+----------------+
 * | content_id      | bigint(20) unsigned  | NO   | PRI | NULL    | auto_increment |
 * | content_size    | int(10) unsigned     | NO   |     | NULL    |                |
 * | content_sha1    | varbinary(32)        | NO   |     | NULL    |                |
 * | content_model   | smallint(5) unsigned | NO   |     | NULL    |                |
 * | content_address | varbinary(255)       | NO   |     | NULL    |                |
 * +-----------------+----------------------+------+-----+---------+----------------+
 * </pre>
 */
@Entity
@Table(name = "content")
public class CommonsContent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the content object
     */
    @Id
    @Column(name = "content_id", nullable = false, length = 20, columnDefinition = "bigint")
    private int id;

    /**
     * Nominal size of the content object (not necessarily of the serialized blob)
     */
    @Column(name = "content_size", nullable = false, length = 10, columnDefinition = "int")
    private int size;

    /**
     * Nominal hash of the content object (not necessarily of the serialized blob)
     */
    @Column(name = "content_sha1", nullable = false, length = 32, columnDefinition = "varbinary")
    private String sha1;

    /**
     * Reference to content_models.model_id. Note the serialization format isn't
     * specified; it should be assumed to be in the default format for the model
     * unless auto-detected otherwise.
     */
    @Column(name = "content_model", nullable = false, length = 5, columnDefinition = "smallint")
    private int model;

    /**
     * URL-like address of the content blob. Usually the structure is: tt:{id} where
     * {id} is a number referencing the text.old_id column. It might be different
     * when using external storage.
     */
    @Column(name = "content_address", nullable = false, length = 255, columnDefinition = "varbinary")
    private String address;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public int getModel() {
        return model;
    }

    public void setModel(int model) {
        this.model = model;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "CommonsContent [id=" + id + ", size=" + size + ", sha1=" + sha1 + ", model=" + model + ", address="
                + address + "]";
    }
}
