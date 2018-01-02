package org.cloudfoundry.credhub.repository;

import org.cloudfoundry.credhub.entity.CredentialVersionData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public interface CredentialVersionRepository extends JpaRepository<CredentialVersionData, UUID> {

  int BATCH_SIZE = 50;

  CredentialVersionData findOneByUuid(UUID uuid);

  @Query(value = "select * from credential_version "
      + "left join certificate_credential on credential_version.uuid = certificate_credential.uuid "
      + "where credential_uuid = ?1 "
      + "and certificate_credential.transitional = false "
      + "order by version_created_at desc "
      + "limit 1", nativeQuery = true)
  CredentialVersionData findLatestNonTransitionalCertificateVersion(UUID credentialUUID);

  @Query(value = "select * from credential_version "
      + "left join certificate_credential on credential_version.uuid = certificate_credential.uuid "
      + "where credential_uuid = ?1 "
      + "and certificate_credential.transitional = true "
      + "order by version_created_at desc "
      + "limit 1", nativeQuery = true)
  CredentialVersionData findTransitionalCertificateVersion(UUID credentialUUID);

  Long countByEncryptedCredentialValueEncryptionKeyUuidNot(UUID encryptionKeyUuid);

  Long countByEncryptedCredentialValueEncryptionKeyUuidIn(List<UUID> encryptionKeyUuids);

  @Query(value = "select new org.cloudfoundry.credhub.repository.EncryptionKeyCountResult(ev.uuid, count(ev.uuid)) "
//      + "from EncryptedValue ev join CredentialVersion cv on cv.encrypted_value_uuid = ev.uuid "
      + "from EncryptedValue ev "
//      + "group by ev.encryptionKeyUuid "
  )
  List<EncryptionKeyCountResult> countGroupByEncryptedCredentialValue();

  Slice<CredentialVersionData> findByEncryptedCredentialValueEncryptionKeyUuidIn(List<UUID> encryptionKeyUuids, Pageable page);

  List<CredentialVersionData> findAllByCredentialUuidOrderByVersionCreatedAtDesc(UUID uuid);

  List<CredentialVersionData> findAllByCredentialUuidAndTypeOrderByVersionCreatedAtDesc(UUID uuid, String credentialType);

  CredentialVersionData findFirstByCredentialUuidOrderByVersionCreatedAtDesc(UUID uuid);
}
