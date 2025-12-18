import React, { useState } from 'react';
import { Box, Button, MenuItem, Select, TextField, InputLabel, FormControl, Typography } from '@mui/material';
import { createCollectionInvite } from '../../api/collectionInvitesApi';

export default function InviteMemberForm({ collectionId }:{ collectionId:string }){
  const [role, setRole] = useState<'EDITOR'|'VIEWER'|'ADMIN'>('EDITOR');
  const [link, setLink] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (e?:React.FormEvent) => {
    e?.preventDefault();
    setLoading(true);
    try{
      const { token } = await createCollectionInvite(collectionId, role);
      const url = `${window.location.origin}/invites/collection/${token}`;
      setLink(url);
    }catch(err:any){
      alert(err.message || 'Failed to create invite');
    }finally{ setLoading(false); }
  };

  return (
    <Box component="form" onSubmit={submit} sx={{ display:'flex', flexDirection:'column', gap:2 }}>
      <FormControl size="small">
        <InputLabel>Role</InputLabel>
        <Select value={role} label="Role" onChange={e=>setRole(e.target.value as any)}>
          <MenuItem value="ADMIN">Admin</MenuItem>
          <MenuItem value="EDITOR">Editor</MenuItem>
          <MenuItem value="VIEWER">Viewer</MenuItem>
        </Select>
      </FormControl>
      <Box sx={{ display:'flex', gap:2 }}>
        <Button type="submit" variant="contained" disabled={loading}>Create invite</Button>
        <Button onClick={()=>{ setLink(null); }}>Reset</Button>
      </Box>
      {link && (
        <Box>
          <Typography>Invite link:</Typography>
          <TextField value={link} fullWidth size="small" InputProps={{ readOnly: true }} />
          <Button onClick={()=>navigator.clipboard.writeText(link)}>Copy</Button>
        </Box>
      )}
    </Box>
  );
}
