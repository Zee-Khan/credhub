package io.pivotal.security.handler;

import io.pivotal.security.auth.UserContext;
import io.pivotal.security.data.AccessControlDataService;
import io.pivotal.security.data.CredentialNameDataService;
import io.pivotal.security.entity.CredentialName;
import io.pivotal.security.exceptions.EntryNotFoundException;
import io.pivotal.security.exceptions.PermissionException;
import io.pivotal.security.request.AccessControlEntry;
import io.pivotal.security.service.PermissionService;
import io.pivotal.security.view.AccessControlListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessControlHandler {
  private final PermissionService permissionService;
  private final AccessControlDataService accessControlDataService;
  private final CredentialNameDataService credentialNameDataService;

  @Autowired
  AccessControlHandler(
      PermissionService permissionService,
      AccessControlDataService accessControlDataService,
      CredentialNameDataService credentialNameDataService
  ) {
    this.permissionService = permissionService;
    this.accessControlDataService = accessControlDataService;
    this.credentialNameDataService = credentialNameDataService;
  }

  public AccessControlListResponse getAccessControlListResponse(UserContext userContext, String name) {
    try {
      final CredentialName credentialName = credentialNameDataService.findOrThrow(name);

      permissionService.verifyAclReadPermission(userContext, name);

      return new AccessControlListResponse(
          credentialName.getName(),
          accessControlDataService.getAccessControlList(credentialName)
      );
    } catch (PermissionException pe){
      // lack of permissions should be indistinguishable from not found.
      throw new EntryNotFoundException("error.resource_not_found");
    }
  }

  public AccessControlListResponse setAccessControlEntries(UserContext userContext, String name, List<AccessControlEntry> accessControlEntryList) {
    final CredentialName credentialName = credentialNameDataService.findOrThrow(name);

    if (!permissionService.hasAclWritePermission(userContext, name)) {
      throw new PermissionException("error.acl.lacks_credential_write");
    }

    accessControlDataService
        .saveAccessControlEntries(credentialName, accessControlEntryList);

    return new AccessControlListResponse(credentialName.getName(), accessControlDataService.getAccessControlList(credentialName));
  }

  public void deleteAccessControlEntries(UserContext userContext, String credentialName, String actor) {
    if (!permissionService.hasAclWritePermission(userContext, credentialName)) {
      throw new EntryNotFoundException("error.acl.lacks_credential_write");
    }

    accessControlDataService.deleteAccessControlEntry(credentialName, actor);
  }
}
