package com.digitalasset.platform.tests.integration.ledger.api

import com.digitalasset.ledger.api.testing.utils.{AkkaBeforeAndAfterAll, IsStatusException, SuiteResourceManagementAroundAll}
import com.digitalasset.ledger.api.v1.admin.package_management_service.PackageManagementServiceGrpc.PackageManagementService
import com.digitalasset.ledger.client.services.admin.PackageManagementClient
import com.digitalasset.platform.apitesting.MultiLedgerFixture
import com.google.protobuf.ByteString
import io.grpc.Status
import org.scalatest.{AsyncFreeSpec, Matchers}
import org.scalatest.concurrent.AsyncTimeLimitedTests

class PackageManagementServiceIT
    extends AsyncFreeSpec
    with AkkaBeforeAndAfterAll
    with MultiLedgerFixture
    with SuiteResourceManagementAroundAll
    with AsyncTimeLimitedTests
    with Matchers {
  override protected def config: Config = Config.default.copy(darFiles = Nil)

  private def packageManagementService(stub: PackageManagementService): PackageManagementClient =
    new PackageManagementClient(stub)

  "should accept packages" in allFixtures { ctx =>
    val client = packageManagementService(ctx.packageManagementService)
    for {
      pkgs <- client.listKnownPackages()
    } yield {
      pkgs.isEmpty shouldBe true
    }
  }

  "fail with the expected status on an invalid upload" in allFixtures { ctx =>
    packageManagementService(ctx.packageManagementService)
      .uploadDarFile(ByteString.EMPTY)
      .failed map { ex => IsStatusException(Status.INVALID_ARGUMENT.getCode)(ex) }
  }

}
