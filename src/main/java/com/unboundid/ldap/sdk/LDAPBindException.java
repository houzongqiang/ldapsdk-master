/*
 * Copyright 2015-2018 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2015-2018 Ping Identity Corporation
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
package com.unboundid.ldap.sdk;



import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.util.NotExtensible;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class defines an exception that can be thrown if the server sends a bind
 * response with a result code other than {@link ResultCode#SUCCESS}, which
 * indicates that the bind operation did not complete successfully.  This may be
 * used to obtain access to any server SASL credentials contained in the
 * non-successful bind result.
 */
@NotExtensible()
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public class LDAPBindException
       extends LDAPException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 6545956074186731236L;



  // The bind result for this exception.
  private final BindResult bindResult;



  /**
   * Creates a new LDAP bind exception from the provided bind result.
   *
   * @param  bindResult  The bind result to use to create this exception.
   */
  public LDAPBindException(final BindResult bindResult)
  {
    super(bindResult);

    this.bindResult = bindResult;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public LDAPResult toLDAPResult()
  {
    return bindResult;
  }



  /**
   * Retrieves the bind result that was returned by the server.
   *
   * @return  The bind result that was returned by the server.
   */
  public BindResult getBindResult()
  {
    return bindResult;
  }



  /**
   * Retrieves the server SASL credentials included in the bind result, if any.
   *
   * @return  The server SASL credentials included in the bind result, or
   *          {@code null} if the bind result did not include any server SASL
   *          credentials.
   */
  public ASN1OctetString getServerSASLCredentials()
  {
    return bindResult.getServerSASLCredentials();
  }
}
