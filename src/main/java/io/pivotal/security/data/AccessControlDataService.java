package io.pivotal.security.data;

import io.pivotal.security.entity.AccessEntryData;
import io.pivotal.security.entity.CredentialName;
import io.pivotal.security.repository.AccessEntryRepository;
import io.pivotal.security.request.AccessControlEntry;
import io.pivotal.security.request.AccessControlOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class AccessControlDataService {

  private AccessEntryRepository accessEntryRepository;
  private final CredentialNameDataService credentialNameDataService;

  @Autowired
  public AccessControlDataService(
      AccessEntryRepository accessEntryRepository,
      CredentialNameDataService credentialNameDataService
  ) {
    this.accessEntryRepository = accessEntryRepository;
    this.credentialNameDataService = credentialNameDataService;
  }

  public List<AccessControlEntry> getAccessControlList(CredentialName credentialName) {
    return createViewsForAllAcesWithName(credentialName);
  }

  public void saveAccessControlEntries(
      CredentialName credentialName,
      List<AccessControlEntry> entries
  ) {
    List<AccessEntryData> existingAccessEntries = accessEntryRepository
        .findAllByCredentialNameUuid(credentialName.getUuid());

    for (AccessControlEntry ace : entries) {
      upsertAccessEntryOperations(credentialName, existingAccessEntries, ace.getActor(),
          ace.getAllowedOperations());
    }
  }

  public List<AccessControlOperation> getAllowedOperations(String name, String actor) {
    List<AccessControlOperation> operations = newArrayList();
    CredentialName credentialName = credentialNameDataService.find(name);
    AccessEntryData accessEntryData = accessEntryRepository.findByCredentialNameAndActor(credentialName, actor);

    if (accessEntryData != null) {
      if (accessEntryData.hasReadPermission()) {
        operations.add(AccessControlOperation.READ);
      }
      if (accessEntryData.hasWritePermission()) {
        operations.add(AccessControlOperation.WRITE);
      }
      if (accessEntryData.hasDeletePermission()) {
        operations.add(AccessControlOperation.DELETE);
      }
      if (accessEntryData.hasReadAclPermission()) {
        operations.add(AccessControlOperation.READ_ACL);
      }
      if (accessEntryData.hasWriteAclPermission()) {
        operations.add(AccessControlOperation.WRITE_ACL);
      }
    }

    return operations;
  }

  public void deleteAccessControlEntry(String name, String actor) {
    CredentialName credentialName = credentialNameDataService.find(name);
    accessEntryRepository.deleteByCredentialNameAndActor(credentialName, actor);
  }

  public boolean hasReadAclPermission(String actor, String name) {
    CredentialName credentialName = credentialNameDataService.find(name);
    final AccessEntryData accessEntryData =
        accessEntryRepository.findByCredentialNameAndActor(credentialName, actor);
    return accessEntryData != null && accessEntryData.hasReadAclPermission();
  }

  public boolean hasAclWritePermission(String actor, String name) {
    CredentialName credentialName = credentialNameDataService.find(name);
    final AccessEntryData accessEntryData =
        accessEntryRepository.findByCredentialNameAndActor(credentialName, actor);
    return accessEntryData != null && accessEntryData.hasWriteAclPermission();
  }

  public boolean hasReadPermission(String actor, String name) {
    CredentialName credentialName = credentialNameDataService.find(name);
    AccessEntryData accessEntryData =
        accessEntryRepository.findByCredentialNameAndActor(credentialName, actor);
    return accessEntryData != null && accessEntryData.hasReadPermission();
  }

  public boolean hasCredentialWritePermission(String actor, String name) {
    CredentialName credentialName = credentialNameDataService.find(name);
    AccessEntryData accessEntryData =
        accessEntryRepository.findByCredentialNameAndActor(credentialName, actor);
    return accessEntryData != null && accessEntryData.hasWritePermission();
  }

  public boolean hasCredentialDeletePermission(String actor, String name) {
    CredentialName credentialName = credentialNameDataService.find(name);
    AccessEntryData accessEntryData =
        accessEntryRepository.findByCredentialNameAndActor(credentialName, actor);
    return accessEntryData != null && accessEntryData.hasDeletePermission();
  }

  private void upsertAccessEntryOperations(CredentialName credentialName,
      List<AccessEntryData> accessEntries, String actor, List<AccessControlOperation> operations) {
    AccessEntryData entry = findAccessEntryForActor(accessEntries, actor);

    if (entry == null) {
      entry = new AccessEntryData(credentialName, actor);
    }

    entry.enableOperations(operations);
    accessEntryRepository.saveAndFlush(entry);
  }

  private AccessControlEntry createViewFor(AccessEntryData data) {
    if (data == null ) {
      return null;
    }
    AccessControlEntry entry = new AccessControlEntry();
    List<AccessControlOperation> operations = data.generateAccessControlOperations();
    entry.setAllowedOperations(operations);
    entry.setActor(data.getActor());
    return entry;
  }

  private List<AccessControlEntry> createViewsForAllAcesWithName(CredentialName credentialName) {
    return accessEntryRepository.findAllByCredentialNameUuid(credentialName.getUuid())
        .stream()
        .map(this::createViewFor)
        .collect(Collectors.toList());
  }

  private AccessEntryData findAccessEntryForActor(List<AccessEntryData> accessEntries,
      String actor) {
    Optional<AccessEntryData> temp = accessEntries.stream()
        .filter(accessEntryData -> accessEntryData.getActor().equals(actor))
        .findFirst();
    return temp.orElse(null);
  }
}
