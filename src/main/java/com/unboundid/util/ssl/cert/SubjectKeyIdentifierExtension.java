/*
 * Copyright 2017-2018 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2017-2018 Ping Identity Corporation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.util.ssl.cert;



import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.util.Debug;
import com.unboundid.util.NotMutable;
import com.unboundid.util.OID;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.util.ssl.cert.CertMessages.*;



/**
 * This class provides an implementation of the subject key identifier X.509
 * certificate extension as described in
 * <A HREF="https://www.ietf.org/rfc/rfc5280.txt">RFC 5280</A> section 4.2.1.2.
 * The OID for this extension is 2.5.29.14.  The value is an octet string and is
 * intended to identify the public key used by a certificate.  The actual format
 * of the key identifier is not specified, although RFC 5280 does specify a
 * couple of possibilities.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class SubjectKeyIdentifierExtension
       extends X509CertificateExtension
{
  /**
   * The OID (2.5.29.14) for subject key identifier extensions.
   */
  public static final OID SUBJECT_KEY_IDENTIFIER_OID = new OID("2.5.29.14");



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -7175921866230880172L;



  // The key identifier for this extension.
  private final ASN1OctetString keyIdentifier;



  /**
   * Creates a new subject key identifier extension with the provided
   * information.
   *
   * @param  isCritical     Indicates whether this extension should be
   *                        considered critical.
   * @param  keyIdentifier  The key identifier for this extension.  It must not
   *                        be {@code null}.
   */
  SubjectKeyIdentifierExtension(final boolean isCritical,
                                final ASN1OctetString keyIdentifier)
  {
    super(SUBJECT_KEY_IDENTIFIER_OID, isCritical,
         keyIdentifier.encode());

    this.keyIdentifier = keyIdentifier;
  }



  /**
   * Creates a new subject key identifier extension from the provided generic
   * extension.
   *
   * @param  extension  The extension to decode as a subject key identifier
   *                    extension.
   *
   * @throws  CertException  If the provided extension cannot be decoded as a
   *                         subject alternative name extension.
   */
  SubjectKeyIdentifierExtension(final X509CertificateExtension extension)
       throws CertException
  {
    super(extension);

    try
    {
      keyIdentifier = ASN1OctetString.decodeAsOctetString(extension.getValue());
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      throw new CertException(
           ERR_SUBJECT_KEY_ID_EXTENSION_CANNOT_PARSE.get(
                String.valueOf(extension), StaticUtils.getExceptionMessage(e)),
           e);
    }
  }



  /**
   * Retrieves the key identifier for this extension.
   *
   * @return  The key identifier for this extension.
   */
  public ASN1OctetString getKeyIdentifier()
  {
    return keyIdentifier;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getExtensionName()
  {
    return INFO_SUBJECT_KEY_IDENTIFIER_EXTENSION_NAME.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("SubjectKeyIdentifierExtension(oid='");
    buffer.append(getOID());
    buffer.append(", isCritical=");
    buffer.append(isCritical());
    buffer.append(", identifierBytes='");
    StaticUtils.toHex(keyIdentifier.getValue(), ":", buffer);
    buffer.append("')");
  }
}
