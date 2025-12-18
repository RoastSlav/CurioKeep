import React, { useState } from 'react';
import { Box, Table, TableHead, TableRow, TableCell, TableBody, Select, MenuItem, IconButton, Chip } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import InviteMemberForm from './InviteMemberForm';
import ConfirmRemoveMemberDialog from './ConfirmRemoveMemberDialog';
import { useCollectionMembers } from '../../hooks/useCollectionMembers';

export default function MembersSection({ collectionId, currentUserRole }:{ collectionId:string; currentUserRole:string }){
  const { members, loading, error, changeRole, remove } = useCollectionMembers(collectionId);
  const [confirm, setConfirm] = useState<{open:boolean; memberName?:string; userId?:string}>({ open:false });

  return (
    <Box sx={{ display:'flex', gap:4 }}>
      <Box sx={{ flex: 1 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Role</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {members.map(m => (
              <TableRow key={m.userId}>
                <TableCell>{m.displayName}</TableCell>
                <TableCell>{m.email}</TableCell>
                <TableCell><Chip label={m.role} /></TableCell>
                <TableCell>
                  { (currentUserRole === 'OWNER' || currentUserRole === 'ADMIN') && m.role !== 'OWNER' && (
                    <>
                      <Select value={m.role} size="small" onChange={async (e)=>{ try{ await changeRole(m.userId, e.target.value as string); }catch(err){ alert('Failed to update role'); } }}>
                        <MenuItem value="ADMIN">Admin</MenuItem>
                        <MenuItem value="EDITOR">Editor</MenuItem>
                        <MenuItem value="VIEWER">Viewer</MenuItem>
                      </Select>
                      <IconButton onClick={()=>setConfirm({ open:true, memberName: m.displayName, userId: m.userId })}><DeleteIcon/></IconButton>
                    </>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>
      <Box sx={{ width: 320 }}>
        <InviteMemberForm collectionId={collectionId} />
      </Box>

      <ConfirmRemoveMemberDialog open={confirm.open} onClose={()=>setConfirm({ open:false })} onConfirm={async ()=>{ if(confirm.userId) await remove(confirm.userId); }} memberName={confirm.memberName||''} />
    </Box>
  );
}
