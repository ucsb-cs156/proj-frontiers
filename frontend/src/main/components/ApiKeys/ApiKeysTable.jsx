import React, { useState } from "react";
import { Badge, Button } from "react-bootstrap";
import { createColumnHelper } from "@tanstack/react-table";
import { toast } from "react-toastify";

import OurTable from "main/components/OurTable";
import ConfirmationModal from "main/components/Common/ConfirmationModal";
import { useBackendMutation } from "main/utils/useBackend";
import { formatTime } from "main/utils/dateUtils";
import {
  apiKeysQueryKey,
  daysUntil,
  describeKey,
  EXPIRY_WARNING_DAYS,
} from "main/utils/apiKeyUtils";

const STATUS_VARIANTS = {
  ACTIVE: "success",
  EXPIRED: "secondary",
  REVOKED: "danger",
};

export default function ApiKeysTable({
  apiKeys,
  courseId,
  testIdPrefix = "ApiKeysTable",
}) {
  const [showRevokeModal, setShowRevokeModal] = useState(false);
  const [keyToRevoke, setKeyToRevoke] = useState(null);

  const queryKey = apiKeysQueryKey(courseId);

  const objectToAxiosParamsRevoke = (apiKey) => ({
    url: "/api/courses/key",
    method: "DELETE",
    params: { courseId: courseId, id: apiKey.id },
  });

  const onRevokeSuccess = (_data, apiKey) => {
    toast(`API key ${describeKey(apiKey)} revoked.`);
  };

  const revokeMutation = useBackendMutation(
    objectToAxiosParamsRevoke,
    { onSuccess: onRevokeSuccess },
    [queryKey],
  );

  const revokeCallback = (cell) => {
    setKeyToRevoke(cell.row.original);
    setShowRevokeModal(true);
  };

  const confirmRevoke = () => {
    revokeMutation.mutate(keyToRevoke);
  };

  const columnHelper = createColumnHelper();

  const columns = [
    {
      header: "Label",
      accessorKey: "label",
    },
    {
      header: "Key",
      id: "keySuffix",
      accessorFn: (row) => `…${row.keySuffix}`,
    },
    {
      header: "Created by",
      accessorKey: "createdByEmail",
    },
    {
      header: "Created",
      id: "createdAt",
      accessorFn: (row) => formatTime(row.createdAt),
    },
    {
      header: "Expires",
      accessorKey: "expiresAt",
      cell: ({ cell }) => {
        const apiKey = cell.row.original;
        const days = daysUntil(apiKey.expiresAt);
        const warn = apiKey.status === "ACTIVE" && days <= EXPIRY_WARNING_DAYS;
        return (
          <>
            {formatTime(apiKey.expiresAt)}
            {warn && (
              <Badge
                bg="warning"
                text="dark"
                className="ms-2"
                data-testid={`${testIdPrefix}-row-${cell.row.index}-expiry-warning`}
              >
                expires in {days} days
              </Badge>
            )}
          </>
        );
      },
    },
    {
      header: "Last used",
      id: "lastUsedAt",
      accessorFn: (row) =>
        row.lastUsedAt ? formatTime(row.lastUsedAt) : "never",
    },
    {
      header: "Uses",
      accessorKey: "usageCount",
    },
    {
      header: "Status",
      accessorKey: "status",
      cell: ({ cell }) => (
        <Badge bg={STATUS_VARIANTS[cell.getValue()]}>{cell.getValue()}</Badge>
      ),
    },
    columnHelper.display({
      id: "Revoke",
      header: "Revoke",
      cell: ({ cell }) =>
        cell.row.original.status === "ACTIVE" ? (
          <Button
            variant="danger"
            onClick={() => revokeCallback(cell)}
            data-testid={`${testIdPrefix}-cell-row-${cell.row.index}-col-Revoke-button`}
          >
            Revoke
          </Button>
        ) : null,
    }),
  ];

  return (
    <>
      <ConfirmationModal
        showModal={showRevokeModal}
        setShowModal={setShowRevokeModal}
        onYes={confirmRevoke}
      >
        <span data-testid={`${testIdPrefix}-revoke-confirmation-message`}>
          Are you sure you want to revoke the API key{" "}
          {keyToRevoke && describeKey(keyToRevoke)}? Anything using it will stop
          working immediately. This cannot be undone.
        </span>
      </ConfirmationModal>
      <OurTable data={apiKeys} columns={columns} testid={testIdPrefix} />
    </>
  );
}
